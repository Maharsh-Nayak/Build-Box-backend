package com.BuildBox.BuildServer.service;

import com.BuildBox.BuildServer.util.CommandRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Nginx reverse proxy routing for deployed applications.
 * Creates routes like: projectId.localhost:8080 → actualHost:dynamicPort
 */
@Service
public class NginxRoutingService {

    private static final Path NGINX_CONF_DIR = Path.of("/tmp/nginx/conf.d");
    private static final Path NGINX_CONTAINER_NAME = Path.of("buildbox-nginx");

    private final CommandRunner commandRunner;
    private final ConcurrentHashMap<String, RouteInfo> activeRoutes = new ConcurrentHashMap<>();

    public NginxRoutingService(CommandRunner commandRunner) {
        this.commandRunner = commandRunner;
        initNginx();
        startNginxContainer();
    }

    /**
     * Initialize Nginx Docker container if not running.
     */
    private void initNginx() {
        try {
            // Create config directory
            Files.createDirectories(NGINX_CONF_DIR);

            // Create main nginx.conf if it doesn't exist
            Path mainConf = NGINX_CONF_DIR.getParent().resolve("nginx.conf");
            if (!Files.exists(mainConf)) {
                String defaultConf = """
                        events {
                            worker_connections 1024;
                        }

                        http {
                            include /etc/nginx/conf.d/*.conf;

                            # Default server
                            server {
                                listen 8080 default_server;
                                server_name _;

                                location / {
                                    return 404 'No route configured for this host';
                                }
                            }
                        }
                        """;
                Files.writeString(mainConf, defaultConf);
            }

            System.out.println("📦 Nginx routing service initialized");
            System.out.println("   Config dir: " + NGINX_CONF_DIR);

        } catch (IOException e) {
            System.err.println("⚠️ Failed to initialize Nginx config directory: " + e.getMessage());
        }
    }

    /**
     * Add a route for a project.
     * Creates: projectId.localhost → host:port
     */
    public void addRoute(String projectId, String host, int port) {
        try {
            String serverName = projectId + ".localhost";
            String upstream = host + ":" + port;

            String config = String.format("""
                    # Route for %s
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
                        }
                    }
                    """, projectId, serverName, upstream);

            Path confFile = NGINX_CONF_DIR.resolve(projectId + ".conf");
            Files.writeString(confFile, config, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            activeRoutes.put(projectId, new RouteInfo(serverName, host, port, confFile.toString()));

            // Reload Nginx
            reloadNginx();

            System.out.println("🔀 Route added: " + serverName + " → " + upstream);

        } catch (IOException e) {
            System.err.println("❌ Failed to add route for " + projectId + ": " + e.getMessage());
        }
    }

    /**
     * Remove a route for a project.
     */
    public void removeRoute(String projectId) {
        try {
            Path confFile = NGINX_CONF_DIR.resolve(projectId + ".conf");

            if (Files.exists(confFile)) {
                Files.delete(confFile);
                activeRoutes.remove(projectId);

                // Reload Nginx
                reloadNginx();

                System.out.println("🔀 Route removed: " + projectId);
            }

        } catch (IOException e) {
            System.err.println("❌ Failed to remove route for " + projectId + ": " + e.getMessage());
        }
    }

    /**
     * Reload Nginx configuration.
     */
    private void reloadNginx() {
        try {
            // Check if container is running
            commandRunner.run(
                    "docker exec buildbox-nginx nginx -s reload 2>/dev/null || echo 'Nginx container not running - routes saved for next start'");
        } catch (Exception e) {
            System.out.println("⚠️ Nginx reload skipped (container may not be running)");
        }
    }

    /**
     * Start Nginx container (call once during setup).
     */
    public void startNginxContainer() {
        try {
            String command = String.format(
                    "docker run -d --name buildbox-nginx " +
                            "-p 8080:8080 " +
                            "-v %s:/etc/nginx/nginx.conf:ro " +
                            "-v %s:/etc/nginx/conf.d:ro " +
                            "nginx:alpine 2>/dev/null || docker start buildbox-nginx",
                    NGINX_CONF_DIR.getParent().resolve("nginx.conf"),
                    NGINX_CONF_DIR);
            commandRunner.run(command);
            System.out.println("✅ Nginx container started on port 8080");
        } catch (Exception e) {
            System.err.println("⚠️ Failed to start Nginx: " + e.getMessage());
        }
    }

    /**
     * Get route URL for a project.
     */
    public String getRouteUrl(String projectId) {
        RouteInfo info = activeRoutes.get(projectId);
        return info != null ? "http://" + info.serverName + ":8080" : null;
    }

    /**
     * Get all active routes.
     */
    public ConcurrentHashMap<String, RouteInfo> getActiveRoutes() {
        return activeRoutes;
    }

    // Route info record
    public record RouteInfo(String serverName, String targetHost, int targetPort, String configPath) {
    }
}
