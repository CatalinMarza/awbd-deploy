package com.artgallery.controller;

import com.artgallery.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Authentication helper endpoints for the SPA.
 *
 * <p>Login ({@code POST /api/auth/login}) and logout ({@code POST /api/auth/logout})
 * are handled by the Spring Security filter chain. This controller only exposes
 * {@code GET /api/auth/me} so the frontend can discover the current session.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "Not authenticated", List.of("No active session")));
        }
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        Map<String, Object> data = Map.of("username", authentication.getName(), "roles", roles);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
