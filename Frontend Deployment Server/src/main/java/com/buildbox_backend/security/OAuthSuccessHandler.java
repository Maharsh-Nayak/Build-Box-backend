package com.buildbox_backend.security;

import com.buildbox_backend.model.User;
import com.buildbox_backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {

        OAuth2User principal = (OAuth2User) authentication.getPrincipal();

        String provider = ((OAuth2AuthenticationToken) authentication)
                .getAuthorizedClientRegistrationId()
                .toUpperCase();

        String email = principal.getAttribute("email");
        if (email == null) email = principal.getAttribute("login");

        String name = principal.getAttribute("name");
        String avatar = principal.getAttribute("avatar_url");

        String providerId =
                provider.equals("GOOGLE")
                        ? principal.getAttribute("sub")
                        : String.valueOf(principal.getAttribute("id"));

        // -------------- Make them EFFECTIVELY FINAL --------------
        final String finalEmail = email;
        final String finalName = name;
        final String finalAvatar = avatar;
        final String finalProvider = provider;
        final String finalProviderId = providerId;

        // ---------- CREATE OR UPDATE USER ----------
        User user = userRepository.findByEmail(finalEmail)
                .map(existing -> {
                    boolean changed = false;

                    if (finalName != null && !finalName.equals(existing.getName())) {
                        existing.setName(finalName);
                        changed = true;
                    }

                    if (finalAvatar != null &&
                            (existing.getAvatarUrl() == null ||
                                    !finalAvatar.equals(existing.getAvatarUrl()))) {
                        existing.setAvatarUrl(finalAvatar);
                        changed = true;
                    }

//                    if (!finalProvider.equals(existing.getProvider())) {
//                        existing.setProvider(finalProvider);
//                        changed = true;
//                    }

                    if (changed) {
                        return userRepository.save(existing);
                    }

                    return existing;
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(finalEmail);
                    newUser.setName(finalName);
                    newUser.setAvatarUrl(finalAvatar);
//                    newUser.setProvider(finalProvider);
//                    newUser.setProviderId(finalProviderId);
                    newUser.setEmailVerified(true);
                    return userRepository.save(newUser);
                });

        // ---------- JWT ----------
        String token = jwtService.generateToken(user.getEmail());

        response.setContentType("application/json");
        response.getWriter().write("""
            { "token": "%s" }
            """.formatted(token));
        response.sendRedirect("/dashboard");
    }

}
