package com.lucasdominato.securefilemanager.security;

import com.lucasdominato.securefilemanager.exception.InvalidJwtAuthenticationException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String JWT_TOKEN_IS_MISSING = "JWT token is missing";
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            setUnauthorizedError(request, JWT_TOKEN_IS_MISSING);
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        String jwtToken = authorizationHeader.substring(7);

        try {
            Claims claims = jwtUtil.getValidClaimsFromToken(jwtToken);
            String username = claims.getSubject();

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String email = (String) claims.get("email");
                String name = (String) claims.get("name");
                String dateOfBirth = (String) claims.get("dateOfBirth");

                CustomUserDetails userDetails = new CustomUserDetails(username, email, name, dateOfBirth);

                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        Collections.emptyList()
                );
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }

            filterChain.doFilter(request, response);
        } catch (InvalidJwtAuthenticationException ex) {
            SecurityContextHolder.clearContext();
            setUnauthorizedError(request, ex.getMessage());
            filterChain.doFilter(request, response);
        }
    }

    private void setUnauthorizedError(HttpServletRequest request, String message) {
        request.setAttribute("authExceptionMessage", message);
    }
}