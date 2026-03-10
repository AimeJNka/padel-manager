package be.ephec.padelmanager.config;

import be.ephec.padelmanager.service.IJwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final IJwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Lire le header Authorization
        String authHeader = request.getHeader("Authorization");

        // 2. Si pas de header ou pas "Bearer " → on passe sans rien faire
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraire le token (enlever "Bearer ")
        String token = authHeader.substring(7);

        try {
            // 4. Extraire le matricule depuis le token
            String matricule = jwtService.extractMatricule(token);

            // 5. Si on a un matricule et qu'on n'est pas déjà authentifié
            if (matricule != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 6. Valider le token
                if (jwtService.isTokenValide(token, matricule)) {

                    // 7. Dire à Spring Security : cet utilisateur est authentifié
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(matricule, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (io.jsonwebtoken.JwtException e) {
            // Token invalide ou expiré — on ne fait rien, Spring Security retournera 401
        }

        // 8. Toujours continuer la chaîne de filtres
        filterChain.doFilter(request, response);
    }
}
