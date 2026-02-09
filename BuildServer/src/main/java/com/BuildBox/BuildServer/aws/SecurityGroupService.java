package com.BuildBox.BuildServer.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages EC2 security group rules for dynamic port access.
 * Automatically opens ports when tasks start and closes them when tasks stop.
 */
@Service
public class SecurityGroupService {

    @Value("${ecs.security.group.id}")
    private String securityGroupId;

    private final Ec2Client ec2;

    // Track opened ports and their rule IDs for cleanup
    private final ConcurrentHashMap<Integer, String> openedPorts = new ConcurrentHashMap<>();

    public SecurityGroupService() {
        this.ec2 = Ec2Client.create();
    }

    /**
     * Open a port for inbound traffic from anywhere (0.0.0.0/0).
     * Used when a new task starts and gets a dynamic port assigned.
     */
    public void openPort(int port) {
        try {
            System.out.println("🔓 Opening security group port: " + port);

            AuthorizeSecurityGroupIngressRequest request = AuthorizeSecurityGroupIngressRequest.builder()
                    .groupId(securityGroupId)
                    .ipPermissions(IpPermission.builder()
                            .ipProtocol("tcp")
                            .fromPort(port)
                            .toPort(port)
                            .ipRanges(IpRange.builder()
                                    .cidrIp("0.0.0.0/0")
                                    .description("BuildBox auto-opened for ECS task")
                                    .build())
                            .build())
                    .build();

            AuthorizeSecurityGroupIngressResponse response = ec2.authorizeSecurityGroupIngress(request);

            // Store the rule ID for later cleanup
            if (response.securityGroupRules() != null && !response.securityGroupRules().isEmpty()) {
                String ruleId = response.securityGroupRules().get(0).securityGroupRuleId();
                openedPorts.put(port, ruleId);
                System.out.println("✅ Port " + port + " opened (Rule ID: " + ruleId + ")");
            }

        } catch (Ec2Exception e) {
            if (e.getMessage().contains("already exists")) {
                System.out.println("⚠️ Port " + port + " already open in security group");
            } else {
                System.err.println("❌ Failed to open port " + port + ": " + e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Close a previously opened port.
     * Used when a task stops to clean up security group rules.
     */
    public void closePort(int port) {
        try {
            System.out.println("🔒 Closing security group port: " + port);

            RevokeSecurityGroupIngressRequest request = RevokeSecurityGroupIngressRequest.builder()
                    .groupId(securityGroupId)
                    .ipPermissions(IpPermission.builder()
                            .ipProtocol("tcp")
                            .fromPort(port)
                            .toPort(port)
                            .ipRanges(IpRange.builder()
                                    .cidrIp("0.0.0.0/0")
                                    .build())
                            .build())
                    .build();

            ec2.revokeSecurityGroupIngress(request);
            openedPorts.remove(port);

            System.out.println("✅ Port " + port + " closed");

        } catch (Ec2Exception e) {
            if (e.getMessage().contains("does not exist")) {
                System.out.println("⚠️ Port " + port + " rule not found (already closed?)");
                openedPorts.remove(port);
            } else {
                System.err.println("❌ Failed to close port " + port + ": " + e.getMessage());
            }
        }
    }

    /**
     * Check if a port is currently opened by this service.
     */
    public boolean isPortOpened(int port) {
        return openedPorts.containsKey(port);
    }

    /**
     * Get the count of currently opened ports.
     */
    public int getOpenedPortCount() {
        return openedPorts.size();
    }
}
