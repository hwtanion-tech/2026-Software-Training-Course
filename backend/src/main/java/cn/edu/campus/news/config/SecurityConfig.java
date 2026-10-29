package cn.edu.campus.news.config;

import cn.edu.campus.news.common.ApiResponse;
import cn.edu.campus.news.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import java.io.IOException;

@Configuration
public class SecurityConfig {
    @Bean SessionRegistry sessionRegistry() { return new SessionRegistryImpl(); }
    @Bean HttpSessionEventPublisher httpSessionEventPublisher() { return new HttpSessionEventPublisher(); }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService userDetailsService(UserRepository users) {
        return username -> {
            var user = users.byUsername(username).orElseThrow(() -> new UsernameNotFoundException("认证失败"));
            return User.withUsername(user.username()).password(user.passwordHash()).roles(user.role())
                    .disabled(!user.enabled()).build();
        };
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper,SessionRegistry registry) throws Exception {
        // 保留默认CSRF保护。Vue通过/api/auth/csrf获取令牌，登录后重新获取。
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**", "/api/auth/csrf", "/api/auth/me", "/api/auth/login",
                        "/api/health", "/api/project", "/error").permitAll()
                .requestMatchers("/api/admin/**", "/api/health/database").hasRole("ADMIN")
                .requestMatchers("/api/manage/**").hasAnyRole("ADMIN", "PUBLISHER")
                .anyRequest().denyAll())
            .formLogin(form -> form.loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, auth) -> json(mapper,response,200,ApiResponse.ok(null)))
                .failureHandler((request,response,ex) -> json(mapper,response,401,
                        ApiResponse.error("UNAUTHORIZED", "账号或密码错误，或账号已停用"))))
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request,response,auth) -> json(mapper,response,200,ApiResponse.ok(null))))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request,response,ex) -> json(mapper,response,401,
                        ApiResponse.error("UNAUTHORIZED", "请先登录")))
                .accessDeniedHandler((request,response,ex) -> json(mapper,response,403,
                        ApiResponse.error("FORBIDDEN", "无操作权限或安全令牌已失效，请刷新后重试"))))
            .sessionManagement(session -> session.maximumSessions(-1).sessionRegistry(registry)
                .expiredSessionStrategy(event -> json(mapper,event.getResponse(),401,
                        ApiResponse.error("SESSION_EXPIRED","账号状态、权限或密码已变更，请重新登录"))))
            .requestCache(cache -> cache.disable());
        return http.build();
    }
    private static void json(ObjectMapper mapper, HttpServletResponse response, int status, Object value) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        mapper.writeValue(response.getWriter(), value);
    }
}
