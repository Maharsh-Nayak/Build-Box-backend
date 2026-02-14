package com.BuildBox.BuildServer.dto;

public record RoutingDetails(
        String mode, // "alb" or "direct"
        String targetUrl, // For ALB: http://[ALB_DNS]
        String hostHeader, // For ALB: api.[project].[domain]
        String host, // For direct: EC2 IP / localhost
        Integer port // For direct: dynamic port
) {
    public static RoutingDetails alb(String targetUrl, String hostHeader) {
        return new RoutingDetails("alb", targetUrl, hostHeader, null, null);
    }

    public static RoutingDetails direct(String host, Integer port) {
        return new RoutingDetails("direct", null, null, host, port);
    }
}
