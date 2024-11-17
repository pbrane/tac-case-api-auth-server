package com.beaconstrategists.tac_case_api_auth_server.config;

import com.beaconstrategists.tac_case_api_auth_server.utils.AuthServerUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import javax.sql.DataSource;
import java.util.Set;
import java.util.function.Consumer;

@Configuration
public class RegisteredClientRepositoryConfig {

    private final Consumer<Set<String>> clientScopes = strings -> {
        strings.add("read.cases");
        strings.add("write.cases");
    };

    @Value("${CLIENT_ID:client-id}")
    private String clientId;

    @Value("${CLIENT_SECRET:{noop}client-secret}")
    private String clientSecret;

    @Value("${CLIENT_NAME:client-name}")
    private String clientName;

    @Bean(name = "InMemoryClientRepo")
    @ConditionalOnProperty(name = "AUTH_SVR_ENV", havingValue = "development", matchIfMissing = true)
    public RegisteredClientRepository devRegisteredClientRepository() {

        //fixme: this needs to be configurable

        RegisteredClient client = AuthServerUtils.createRegisteredClient(
                clientId,
                clientSecret,
                clientName,
                ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                AuthorizationGrantType.CLIENT_CREDENTIALS,
                clientScopes
        );

        return new InMemoryRegisteredClientRepository(client);
    }

    @Bean(name = "JDBCClientRepo")
    @ConditionalOnProperty(name = "AUTH_SVR_ENV", havingValue = "production", matchIfMissing = false)
    public RegisteredClientRepository prodRegisteredClientRepository(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        JdbcRegisteredClientRepository registeredClientRepository = new JdbcRegisteredClientRepository(jdbcTemplate);

        RegisteredClient client = AuthServerUtils.createRegisteredClient(
                clientId,
                clientSecret,
                clientName,
                ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                AuthorizationGrantType.CLIENT_CREDENTIALS,
                clientScopes
        );

        registeredClientRepository.save(client);
        return registeredClientRepository;
    }

}