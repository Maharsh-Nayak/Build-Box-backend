package com.buildbox_backend.service;

import com.buildbox_backend.model.Team;
import com.buildbox_backend.model.TeamMember;
import com.buildbox_backend.model.User;
import com.buildbox_backend.repository.TeamMemberRepository;
import com.buildbox_backend.repository.TeamRepository;
import com.buildbox_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private UserRepository userRepository;

    public Team createTeam(String name, User owner) {
        Team team = new Team();
        team.setName(name);
        team.setOwner(owner);
        team.setCreatedAt(LocalDateTime.now());
        Team saved = teamRepository.save(team);

        // Auto-add owner as OWNER member
        addMember(saved.getId(), owner.getId(), "OWNER");
        return saved;
    }

    public List<Team> getTeamsForUser(Long userId) {
        // Teams the user owns
        List<Team> owned = teamRepository.findByOwnerId(userId);

        // Teams the user is a member of
        List<TeamMember> memberships = teamMemberRepository.findByUserId(userId);
        List<Long> memberTeamIds = memberships.stream()
                .map(tm -> tm.getTeam().getId())
                .toList();

        List<Team> memberTeams = teamRepository.findAllById(memberTeamIds);

        // Merge and deduplicate
        java.util.Set<Long> seenIds = new java.util.HashSet<>();
        List<Team> all = new java.util.ArrayList<>();
        for (Team t : owned) {
            if (seenIds.add(t.getId()))
                all.add(t);
        }
        for (Team t : memberTeams) {
            if (seenIds.add(t.getId()))
                all.add(t);
        }
        return all;
    }

    public Optional<Team> getById(Long id) {
        return teamRepository.findById(id);
    }

    public TeamMember addMember(Long teamId, Long userId, String role) {
        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new RuntimeException("User is already a member of this team");
        }
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found: " + teamId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        member.setRole(role != null ? role : "MEMBER");
        member.setCreatedAt(LocalDateTime.now());
        return teamMemberRepository.save(member);
    }

    public void removeMember(Long teamId, Long userId) {
        teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .ifPresent(teamMemberRepository::delete);
    }

    public List<TeamMember> getMembers(Long teamId) {
        return teamMemberRepository.findByTeamId(teamId);
    }
}
