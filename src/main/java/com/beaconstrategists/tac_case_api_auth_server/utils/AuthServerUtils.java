package com.beaconstrategists.tac_case_api_auth_server.utils;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@ShellComponent
public class AuthServerUtils {

    private static final String QUERY =
            "  SELECT * " +
            "    FROM oauth2_registered_client " +
            "ORDER BY client_secret_expires_at ASC";

    private static final ClientAuthenticationMethod AUTHENTICATION_METHOD = ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
    private static final AuthorizationGrantType GRANT_TYPE = AuthorizationGrantType.CLIENT_CREDENTIALS;

    //fixme: make this configurable
    private final Consumer<Set<String>> clientScopes = strings -> {
        strings.add("read.cases");
        strings.add("write.cases");
    };

    //private final static PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final static PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final RegisteredClientRepository registeredClientRepository;
    private final JdbcTemplate jdbcTemplate;

    public AuthServerUtils(RegisteredClientRepository registeredClientRepository, JdbcTemplate jdbcTemplate) {
        this.registeredClientRepository = registeredClientRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @ShellMethod(key = "registerOrUpdateClient", value = "Register new client or update existing")
    public String registerOrUpdateClient(@ShellOption(value = "-i", defaultValue = "client-id", help = "Client ID") String clientId,
                                         @ShellOption(value = "-s", defaultValue = "client-secret", help = "Client Secret") String clientSecret,
                                         @ShellOption(value = "-n", defaultValue = "msft", help = "Client Name") String clientName){
        RegisteredClient client = createOrUpdate(clientId, clientSecret, clientName, AUTHENTICATION_METHOD, GRANT_TYPE, clientScopes);
        registeredClientRepository.save(client);
        return String.format("Client: %s, registered. Expires: %s", client.getClientName(), client.getClientSecretExpiresAt());
    }

    @ShellMethod(key = "expireClient", value= "Expires an existing client")
    public String expireClient(@ShellOption(value = "-i", defaultValue = "client-app") String clientId) {
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        if (client != null) {
            Instant expiresAt = Instant.now();
            RegisteredClient registeredClient = RegisteredClient.from(client)
                    .clientSecretExpiresAt(expiresAt)
                    .build();
            registeredClientRepository.save(registeredClient);
            return String.format("Client: %s, expired at: %s", clientId, expiresAt);
        } else {
            return String.format("Client not found: %s", clientId);
        }
    }

    //fixme: this should be improved to support dev's inmemory repository
    @ShellMethod(value = "List all registered clients sorted by secret expiration date.", key = "list-clients")
    public String listClients() {
        List<RegisteredClient> clients = jdbcTemplate.query(QUERY, new JdbcRegisteredClientRepository.RegisteredClientRowMapper());
        List<RegisteredClient> sortedClients = clients;
//        List<RegisteredClient> sortedClients = clients.stream()
//                .sorted((client1, client2) -> {
//                    Instant expiresAt1 = client1.getClientSecretExpiresAt();
//                    Instant expiresAt2 = client2.getClientSecretExpiresAt();
//                    if (expiresAt1 == null && expiresAt2 == null) return 0;
//                    if (expiresAt1 == null) return 1;
//                    if (expiresAt2 == null) return -1;
//                    return expiresAt1.compareTo(expiresAt2);
//                })
//                .toList();

        StringBuilder result = new StringBuilder("\nRegistered Clients (sorted by client secret expiration date):\n");
        for (RegisteredClient client : sortedClients) {
            result.append(String.format("Client ID: %s, Client Secret %s, Client Name: %s, Expires At: %s\n",
                    client.getClientId(),
                    client.getClientSecret(),
                    client.getClientName(),
                    client.getClientSecretExpiresAt() != null ? client.getClientSecretExpiresAt().toString() : "Never"));
        }
        return result.toString();
    }

    //fixme: Left off here. This method needs attention and normalized with register method in the Bean's config class
    private RegisteredClient createOrUpdate(String clientId, String clientSecret, String clientName, ClientAuthenticationMethod clientAuthenticationMethod,
                                           AuthorizationGrantType grantType,
                                           Consumer<Set<String>> scopes) {

        RegisteredClient client = registeredClientRepository.findByClientId(clientId);

        if (client == null) {

            if (scopes != null) {
                scopes = clientScopes;
            }

            client = createRegisteredClient(clientId, clientSecret, clientName, clientAuthenticationMethod, grantType, scopes);

        } else {
            client = RegisteredClient.from(client)
//                    .clientSecret(passwordEncoder.encode(clientSecret))
                    .clientSecret("{noop}"+clientSecret)
                    .clientSecretExpiresAt(Instant.now().plus(Duration.ofDays(30)))
                    .build();
        }

        registeredClientRepository.save(client);
        return client;
    }

    public static RegisteredClient createRegisteredClient(String clientId,
                                                  String clientSecret,
                                                  String clientName,
                                                  ClientAuthenticationMethod clientAuthenticationMethod,
                                                  AuthorizationGrantType grantType,
                                                  Consumer<Set<String>> scopes) {

        //fixme: should probably throw exception if any of these are null and/or we should provide defaults where possible
        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret("{noop}"+clientSecret)
//                .clientSecret(passwordEncoder.encode(clientSecret))
                .clientName(clientName)
                .clientAuthenticationMethod(clientAuthenticationMethod)
                .authorizationGrantType(grantType)
                .clientIdIssuedAt(Instant.now())
                .clientSecretExpiresAt(Instant.now().plus(Duration.ofDays(30)))
                .scopes(scopes)
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(5))
                        .build())
                .build();
    }

}
