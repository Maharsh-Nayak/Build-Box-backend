package com.BuildBox.BuildServer.service;

import com.BuildBox.BuildServer.model.Project;
import com.BuildBox.BuildServer.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.*;

@Service
@ConditionalOnProperty(name = "routing.backend", havingValue = "alb")
public class AlbRoutingBackend implements RoutingBackend {

    @Value("${routing.alb.arn:}")
    private String ALB_ARN;

    @Value("${routing.alb.listener.arn:}")
    private String LISTENER_ARN;

    @Value("${routing.alb.dns.host:buildbox.com}")
    private String BASE_DOMAIN;

    private final ElasticLoadBalancingV2Client elb;
    private final Ec2Client ec2;
    private final ProjectRepository projectRepository;
    private final AlbListenerPriorityService priorityService;

    public AlbRoutingBackend(ElasticLoadBalancingV2Client elb,
            Ec2Client ec2,
            ProjectRepository projectRepository,
            AlbListenerPriorityService priorityService) {
        this.elb = elb;
        this.ec2 = ec2;
        this.projectRepository = projectRepository;
        this.priorityService = priorityService;
    }

    @Override
    public void addRoute(String projectId, String host, int port) {
        System.out.println("☁️ ALB: Provisioning route for " + projectId + " on " + host + ":" + port);

        try {
            // Find project entity for priority allocation
            Project project = projectRepository.findBySlug(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

            // 1. Ensure Target Group exists
            String targetGroupName = "bb-" + project.getSlug();
            String targetGroupArn = ensureTargetGroup(targetGroupName);

            // 2. Register Target
            registerTarget(targetGroupArn, host, port);

            // 3. Ensure Listener Rule exists
            String hostHeader = "api." + project.getSlug() + "." + BASE_DOMAIN;
            ensureListenerRule(targetGroupArn, hostHeader, project);

            System.out.println("✅ ALB: Route ready at https://" + hostHeader);
        } catch (Exception e) {
            System.err.println("❌ ALB: Failed to add route: " + e.getMessage());
            throw new RuntimeException("ALB Route Failure", e);
        }
    }

    @Override
    public void removeRoute(String projectId) {
        System.out.println("🗑️ ALB: Removing route for " + projectId);
        // Implementation for removal if needed (rule deletion, TG deregistration)
    }

    @Override
    public String getRouteUrl(String projectId) {
        return "https://api." + projectId + "." + BASE_DOMAIN;
    }

    private String ensureTargetGroup(String name) {
        try {
            DescribeTargetGroupsResponse describe = elb.describeTargetGroups(r -> r.names(name));
            return describe.targetGroups().get(0).targetGroupArn();
        } catch (TargetGroupNotFoundException e) {
            CreateTargetGroupResponse create = elb.createTargetGroup(r -> r
                    .name(name)
                    .protocol(ProtocolEnum.HTTP)
                    .port(80) // Default internal app port
                    .vpcId(getVpcId())
                    .targetType(TargetTypeEnum.INSTANCE) // ALB INSTANCE type requires EC2 instance ID, not IP
                    .healthCheckPath("/")
                    .matcher(Matcher.builder().httpCode("200-499").build())
                    .healthCheckIntervalSeconds(30)
                    .healthyThresholdCount(2)
                    .unhealthyThresholdCount(2));
            return create.targetGroups().get(0).targetGroupArn();
        }
    }

    /**
     * Resolves the EC2 instance ID that owns the given IP (public or private).
     * Required for ALB INSTANCE-type target groups, which do not accept IP
     * addresses.
     * Handles multi-AZ: DescribeInstances is account-scoped within the region.
     */
    private String resolveInstanceIdFromIp(String ip) {
        if (ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("IP address is required to resolve instance ID");
        }
        // Try private IP first (ALB targets instances via VPC; private IP is
        // authoritative)
        String instanceId = findInstanceByFilter("private-ip-address", ip);
        if (instanceId != null) {
            return instanceId;
        }
        instanceId = findInstanceByFilter("ip-address", ip);
        if (instanceId != null) {
            return instanceId;
        }
        throw new RuntimeException(
                "No EC2 instance found for IP '" + ip + "'. Ensure the IP is in this account/region.");
    }

    private String findInstanceByFilter(String filterName, String value) {
        DescribeInstancesResponse response = ec2.describeInstances(DescribeInstancesRequest.builder()
                .filters(Filter.builder().name(filterName).values(value).build())
                .build());
        for (Reservation res : response.reservations()) {
            for (Instance inst : res.instances()) {
                if (software.amazon.awssdk.services.ec2.model.InstanceStateName.RUNNING.equals(inst.state().name())) {
                    return inst.instanceId();
                }
            }
        }
        return null;
    }

    private void registerTarget(String tgArn, String instanceIp, int port) {
        String instanceId = resolveInstanceIdFromIp(instanceIp);
        elb.registerTargets(r -> r
                .targetGroupArn(tgArn)
                .targets(TargetDescription.builder()
                        .id(instanceId)
                        .port(port)
                        .build()));
    }

    private void ensureListenerRule(String tgArn, String hostHeader, Project project) {
        DescribeRulesResponse rulesResponse = elb.describeRules(r -> r.listenerArn(LISTENER_ARN));

        // Check if a rule with the exact host header already exists
        boolean ruleExists = rulesResponse.rules().stream()
                .flatMap(rule -> rule.conditions().stream())
                .filter(c -> "host-header".equals(c.field()))
                .anyMatch(c -> c.hostHeaderConfig().values().contains(hostHeader));

        if (ruleExists) {
            System.out.println("✅ ALB: Listener rule already exists for " + hostHeader);
            return;
        }

        // Allocate deterministic priority using our service
        int priority = priorityService.allocatePriority(LISTENER_ARN, project);

        try {
            createListenerRule(tgArn, hostHeader, priority);
        } catch (PriorityInUseException e) {
            // A stale rule with the same priority exists on AWS — clean it up and retry
            System.out.println("⚠️ ALB: Priority " + priority + " is stale on AWS. Cleaning up...");
            deleteStaleRuleByPriority(rulesResponse, priority);
            createListenerRule(tgArn, hostHeader, priority);
            System.out.println("✅ ALB: Stale rule cleaned and new rule created at priority " + priority);
        }
    }

    private void createListenerRule(String tgArn, String hostHeader, int priority) {
        elb.createRule(r -> r
                .listenerArn(LISTENER_ARN)
                .priority(priority)
                .conditions(RuleCondition.builder()
                        .field("host-header")
                        .hostHeaderConfig(HostHeaderConditionConfig.builder().values(hostHeader).build())
                        .build())
                .actions(Action.builder()
                        .type(ActionTypeEnum.FORWARD)
                        .targetGroupArn(tgArn)
                        .build()));
    }

    /**
     * Finds and deletes a stale ALB listener rule by its priority number.
     * This handles the case where a previous deployment left an orphaned rule on AWS
     * that no longer matches the current host header but occupies the same priority slot.
     */
    private void deleteStaleRuleByPriority(DescribeRulesResponse rulesResponse, int priority) {
        String targetPriority = String.valueOf(priority);

        rulesResponse.rules().stream()
                .filter(rule -> targetPriority.equals(rule.priority()))
                .findFirst()
                .ifPresentOrElse(
                        staleRule -> {
                            System.out.println("🗑️ ALB: Deleting stale rule: " + staleRule.ruleArn()
                                    + " (priority " + priority + ")");
                            elb.deleteRule(r -> r.ruleArn(staleRule.ruleArn()));
                            System.out.println("✅ ALB: Stale rule deleted successfully");
                        },
                        () -> {
                            // The rule wasn't in our cached response — re-fetch and try again
                            System.out.println("⚠️ ALB: Stale rule not in cached response, re-fetching...");
                            DescribeRulesResponse freshRules = elb.describeRules(
                                    r -> r.listenerArn(LISTENER_ARN));
                            freshRules.rules().stream()
                                    .filter(rule -> targetPriority.equals(rule.priority()))
                                    .findFirst()
                                    .ifPresent(staleRule -> {
                                        System.out.println("🗑️ ALB: Deleting stale rule: " + staleRule.ruleArn());
                                        elb.deleteRule(r -> r.ruleArn(staleRule.ruleArn()));
                                        System.out.println("✅ ALB: Stale rule deleted successfully");
                                    });
                        });
    }

    @Override
    public com.BuildBox.BuildServer.dto.RoutingDetails getRoutingDetails(String projectId) {
        String hostHeader = "api." + projectId + "." + BASE_DOMAIN;
        String dns = getAlbDns();
        return com.BuildBox.BuildServer.dto.RoutingDetails.alb("http://" + dns, hostHeader);
    }

    private String albDnsCache;

    private String getAlbDns() {
        if (albDnsCache != null) {
            return albDnsCache;
        }
        try {
            DescribeLoadBalancersResponse response = elb.describeLoadBalancers(r -> r.loadBalancerArns(ALB_ARN));
            albDnsCache = response.loadBalancers().get(0).dnsName();
            return albDnsCache;
        } catch (Exception e) {
            System.err.println("❌ ALB: Failed to discover DNS Name from ALB ARN: " + e.getMessage());
            throw new RuntimeException("Failed to discover ALB DNS", e);
        }
    }

    private String vpcIdCache;

    private String getVpcId() {
        if (vpcIdCache != null) {
            return vpcIdCache;
        }
        try {
            DescribeLoadBalancersResponse response = elb.describeLoadBalancers(r -> r.loadBalancerArns(ALB_ARN));
            vpcIdCache = response.loadBalancers().get(0).vpcId();
            return vpcIdCache;
        } catch (Exception e) {
            System.err.println("❌ ALB: Failed to discover VPC ID from ALB ARN: " + e.getMessage());
            throw new RuntimeException("Failed to discover VPC ID", e);
        }
    }
}
