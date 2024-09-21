package com.example.curtaincall.global.auth.filter;

import com.example.curtaincall.global.auth.jwt.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter{
    private final JwtUtils jwtUtils;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            log.info("doFilterInternal() - if문 통과");
            String token = authorizationHeader.substring(7);
            if(jwtUtils.validateToken(token)) {
                Long id = jwtUtils.getUserId(token);

                /*CustomUserDetails userDetails = customUserDetailsService.loadUserByUsername(id.toString());

                if(userDetails != null) {
                    log.info("doFilterInternal() - userDetalis 존재");
                    //UserDetsils, Password, Role -> 접근권한 인증 Token 생성
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    //현재 Request의 Security Context에 접근권한 설정
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                    log.info("authentication: " + SecurityContextHolder.getContext().getAuthentication());
                }*/
            }
        }

        filterChain.doFilter(request, response);
    }
}
