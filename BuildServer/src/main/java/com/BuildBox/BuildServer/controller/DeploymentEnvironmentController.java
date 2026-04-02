package com.BuildBox.BuildServer.controller;

import com.BuildBox.BuildServer.dto.DeploymentEnvironmentDTO;
import com.BuildBox.BuildServer.dto.EnvironmentVariablesRequest;
import com.BuildBox.BuildServer.model.DeploymentEnvironment;
import com.BuildBox.BuildServer.service.DeploymentEnvironmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/deployment-env")
@CrossOrigin(origins = "*")
public class DeploymentEnvironmentController {
    
    private final DeploymentEnvironmentService envService;
    
    public DeploymentEnvironmentController(DeploymentEnvironmentService envService) {
        this.envService = envService;
    }
    
    /**
     * Save environment variables for a project
     * POST /api/deployment-env/save
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveEnvironmentVariables(
        @RequestBody EnvironmentVariablesRequest request
    ) {
        try {
            String projectId = request.getProjectId();
            String envType = request.getEnvironmentType();
            DeploymentEnvironment.EnvironmentType type = 
                DeploymentEnvironment.EnvironmentType.valueOf(envType.toUpperCase());
            
            // Save all variables
            request.getVariables().forEach(var -> {
                envService.saveEnvironmentVariable(
                    projectId,
                    type,
                    var.getKey(),
                    var.getValue(),
                    var.getIsSecret(),
                    "user" // Can get from security context
                );
            });
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Environment variables saved successfully",
                "count", request.getVariables().size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Get environment variables for a project
     * GET /api/deployment-env/{projectId}/{envType}
     */
    @GetMapping("/{projectId}/{envType}")
    public ResponseEntity<List<DeploymentEnvironmentDTO>> getEnvironmentVariables(
        @PathVariable String projectId,
        @PathVariable String envType
    ) {
        try {
            DeploymentEnvironment.EnvironmentType type = 
                DeploymentEnvironment.EnvironmentType.valueOf(envType.toUpperCase());
            
            List<DeploymentEnvironmentDTO> variables = 
                envService.getEnvironmentVariables(projectId, type)
                    .stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(variables);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get environment variables as key-value map
     * GET /api/deployment-env/{projectId}/{envType}/map
     */
    @GetMapping("/{projectId}/{envType}/map")
    public ResponseEntity<Map<String, String>> getEnvironmentVariablesAsMap(
        @PathVariable String projectId,
        @PathVariable String envType
    ) {
        try {
            DeploymentEnvironment.EnvironmentType type = 
                DeploymentEnvironment.EnvironmentType.valueOf(envType.toUpperCase());
            
            Map<String, String> variables = 
                envService.getEnvironmentVariablesAsMap(projectId, type);
            
            return ResponseEntity.ok(variables);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Delete specific environment variable
     * DELETE /api/deployment-env/{projectId}/{envType}/{key}
     */
    @DeleteMapping("/{projectId}/{envType}/{key}")
    public ResponseEntity<Map<String, String>> deleteEnvironmentVariable(
        @PathVariable String projectId,
        @PathVariable String envType,
        @PathVariable String key
    ) {
        try {
            DeploymentEnvironment.EnvironmentType type = 
                DeploymentEnvironment.EnvironmentType.valueOf(envType.toUpperCase());
            
            envService.deleteEnvironmentVariable(projectId, type, key);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Environment variable deleted"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Delete all environment variables for a specific type
     * DELETE /api/deployment-env/{projectId}/{envType}
     */
    @DeleteMapping("/{projectId}/{envType}")
    public ResponseEntity<Map<String, String>> deleteAllEnvironmentVariables(
        @PathVariable String projectId,
        @PathVariable String envType
    ) {
        try {
            DeploymentEnvironment.EnvironmentType type = 
                DeploymentEnvironment.EnvironmentType.valueOf(envType.toUpperCase());
            
            envService.deleteAllEnvironmentVariables(projectId, type);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "All environment variables deleted"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    // Helper method to convert entity to DTO
    private DeploymentEnvironmentDTO toDTO(DeploymentEnvironment env) {
        DeploymentEnvironmentDTO dto = new DeploymentEnvironmentDTO();
        dto.setId(env.getId());
        dto.setProjectId(env.getProjectId());
        dto.setEnvironmentType(env.getEnvironmentType().toString());
        dto.setKey(env.getKey());
        // Don't include value if it's secret
        if (!env.getIsSecret()) {
            dto.setValue(env.getValue());
        } else {
            dto.setValue("***SECRET***");
        }
        dto.setIsSecret(env.getIsSecret());
        dto.setCreatedAt(env.getCreatedAt());
        dto.setUpdatedAt(env.getUpdatedAt());
        dto.setCreatedBy(env.getCreatedBy());
        return dto;
    }
}
