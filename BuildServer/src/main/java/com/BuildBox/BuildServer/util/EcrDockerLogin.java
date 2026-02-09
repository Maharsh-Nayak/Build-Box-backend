package com.BuildBox.BuildServer.util;

import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.GetAuthorizationTokenResponse;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class EcrDockerLogin {

    private final CommandRunner commandRunner;

    public EcrDockerLogin(CommandRunner commandRunner) {
        this.commandRunner = commandRunner;
    }

    public void login() throws Exception {
        try (EcrClient ecr = EcrClient.create()) {
            GetAuthorizationTokenResponse tokenResponse =
                    ecr.getAuthorizationToken();

            if (tokenResponse.authorizationData().isEmpty()) {
                throw new RuntimeException("No authorization data found");
            }
            
            String token = tokenResponse.authorizationData().get(0).authorizationToken();
            String decoded = new String(Base64.getDecoder().decode(token));
            String[] parts = decoded.split(":");
            if (parts.length < 2) {
                 throw new RuntimeException("Invalid token format");
            }
            String password = parts[1];
            String endpoint = tokenResponse.authorizationData().get(0).proxyEndpoint();

            // Using commandRunner instead of static CommandExecutor
            commandRunner.run(
                    "docker login -u AWS -p " + password + " " + endpoint
            );
        }
    }
}
