package com.buildbox_backend.controller;

import com.buildbox_backend.model.Team;
import com.buildbox_backend.model.TeamMember;
import com.buildbox_backend.model.User;
import com.buildbox_backend.repository.UserRepository;
import com.buildbox_backend.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createTeam(@RequestBody Map<String, String> body, Authentication auth) {
        User user = getUser(auth);
        if (user == null)
            return ResponseEntity.status(401).build();

        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Team name is required"));
        }

        Team team = teamService.createTeam(name, user);
        return ResponseEntity.ok(team);
    }

    @GetMapping
    public ResponseEntity<List<Team>> listTeams(Authentication auth) {
        User user = getUser(auth);
        if (user == null)
            return ResponseEntity.status(401).build();
        return ResponseEntity.ok(teamService.getTeamsForUser(user.getId()));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<TeamMember>> listMembers(@PathVariable("id") Long id) {
        return ResponseEntity.ok(teamService.getMembers(id));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<?> addMember(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
        String email = body.get("email");
        String role = body.get("role");

        User memberUser = userRepository.findByEmail(email).orElse(null);
        if (memberUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found with email: " + email));
        }

        try {
            TeamMember member = teamService.addMember(id, memberUser.getId(), role);
            return ResponseEntity.ok(member);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable("id") Long id, @PathVariable("userId") Long userId) {
        teamService.removeMember(id, userId);
        return ResponseEntity.ok(Map.of("message", "Member removed"));
    }

    private User getUser(Authentication auth) {
        if (auth == null)
            return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }
}
