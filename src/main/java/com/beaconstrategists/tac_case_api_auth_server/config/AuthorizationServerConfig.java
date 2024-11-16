package com.beaconstrategists.tac_case_api_auth_server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

@Configuration
public class AuthorizationServerConfig {

    private final RegisteredClientRepository registeredClientRepository;

    public AuthorizationServerConfig(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

}
