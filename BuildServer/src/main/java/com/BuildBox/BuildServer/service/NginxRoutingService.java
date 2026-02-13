package com.BuildBox.BuildServer.service;

import com.BuildBox.BuildServer.util.CommandRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Nginx reverse proxy routing for deployed applications.
 * Writes declarative configuration files to a system directory and signals
 * Nginx to reload.
 */
@Service
public class NginxRoutingService {

    @Value("${nginx.conf.path:/etc/nginx/sites-enabled}")
    private String NGINX_CONF_DIR;

    private final CommandRunner commandRunner;
    private final ConcurrentHashMap<String, RouteInfo> activeRoutes = new ConcurrentHashMap<>();

    public NginxRoutingService(CommandRunner commandRunner) {
        this.commandRunner = commandRunner;
    }

    /**
     * Add or update a route for a project.
     * Creates: projectId.localhost → host:port
     */
    public void addRoute(String projectId, String host, int port) {
        try {
            // Ensure directory exists (useful for local dev/testing)
            Files.createDirectories(Path.of(NGINX_CONF_DIR));

            String serverName = projectId + ".localhost";
            String upstream = host + ":" + port;

            String config = String.format("""
                    # Dynamic route for project: %s
                    server {
                        listen 8080;
                        server_name %s;

                        location / {
                            proxy_pass http://%s;
                            proxy_http_version 1.1;
                            proxy_set_header Host $host;
                            proxy_set_header X-Real-IP $remote_addr;
                            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                            proxy_set_header Upgrade $http_upgrade;
                            proxy_set_header Connection "upgrade";

                            # Timeouts for cold starts / long running builds
                            proxy_read_timeout 300;
                            proxy_connect_timeout 300;
                        }
                    }
                    """, projectId, serverName, upstream);

            Path confFile = Path.of(NGINX_CONF_DIR, projectId + ".conf");
            Files.writeString(confFile, config, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            activeRoutes.put(projectId, new RouteInfo(serverName, host, port, confFile.toString()));

            // Reload Nginx on host
            reloadNginx();

            System.out.println("🔀 Nginx route updated: " + serverName + " → " + upstream);

        } catch (IOException e) {
            System.err.println("❌ Failed to create Nginx route for " + projectId + ": " + e.getMessage());
        }
    }

    /**
     * Remove a route for a project and update Nginx.
     */
    public void removeRoute(String projectId) {
        try {
            Path confFile = Path.of(NGINX_CONF_DIR, projectId + ".conf");

            if (Files.deleteIfExists(confFile)) {
                activeRoutes.remove(projectId);
                reloadNginx();
                System.out.println("🔀 Nginx route removed: " + projectId);
            }

        } catch (IOException e) {
            System.err.println("❌ Failed to remove Nginx route for " + projectId + ": " + e.getMessage());
        }
    }

    /**
     * Sends reload signal to Nginx host process.
     */
    private void reloadNginx() {
        try {
            // Primary reload command for production systems
            commandRunner.run("nginx -s reload");
        } catch (Exception e) {
            System.err.println("⚠️ Nginx reload signal failed. Ensure Nginx is installed on the host and running.");
        }
    }

    public String getRouteUrl(String projectId) {
        RouteInfo info = activeRoutes.get(projectId);
        return info != null ? "http://" + info.serverName + ":8080" : null;
    }

    public ConcurrentHashMap<String, RouteInfo> getActiveRoutes() {
        return activeRoutes;
    }

    public record RouteInfo(String serverName, String targetHost, int targetPort, String configPath) {
    }
}
