package com.rocket.backend.auth;

import com.rocket.backend.session.SessionService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SessionService sessionService;

    public JwtAuthFilter(JwtService jwtService, SessionService sessionService) {
        this.jwtService = jwtService;
        this.sessionService = sessionService;
    }

    // Don't check tokens on login/register: those are how you GET a token
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // No token? Let it continue. The security rules will block it if the URL needs login.
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);   // remove "Bearer "

        try {
            JwtService.AccessClaims claims = jwtService.parse(token);

            // Is this token's session still the user's current one?
            if (!sessionService.isCurrent(claims.userId(), claims.sessionId())) {
                writeError(response, "KICKED", "Your account was logged in on another device");
                return;
            }

            var auth = new UsernamePasswordAuthenticationToken(claims.userId(), null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (ExpiredJwtException e) {
            writeError(response, "TOKEN_EXPIRED", "Token expired, please refresh");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            writeError(response, "TOKEN_INVALID", "Invalid token");
            return;
        }

        chain.doFilter(request, response);   // pass the request on to the controller
    }

    private void writeError(HttpServletResponse res, String code, String message) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);   // 401
        res.setContentType("application/json");
        res.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}