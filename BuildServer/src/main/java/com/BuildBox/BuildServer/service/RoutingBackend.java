package com.BuildBox.BuildServer.service;

/**
 * Pluggable backend for routing traffic to deployed services.
 */
public interface RoutingBackend {
    /**
     * Registers a new route for a project.
     * 
     * @param projectId The unique project identifier.
     * @param host      The target host (IP or DNS).
     * @param port      The target port on the host.
     */
    void addRoute(String projectId, String host, int port);

    /**
     * Removes an existing route for a project.
     * 
     * @param projectId The unique project identifier.
     */
    void removeRoute(String projectId);

    /**
     * Gets the routing details for the project, adapted for the current backend
     * mode.
     * 
     * @param projectId The unique project identifier.
     * @return RoutingDetails object for the proxy.
     */
    com.BuildBox.BuildServer.dto.RoutingDetails getRoutingDetails(String projectId);

    String getRouteUrl(String projectId);
}
