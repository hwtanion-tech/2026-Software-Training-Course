package cn.edu.campus.news.controller;

import cn.edu.campus.news.common.ApiResponse;
import cn.edu.campus.news.domain.Models.UserView;
import cn.edu.campus.news.service.CurrentUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final CurrentUser current;
    public AuthController(CurrentUser current) { this.current = current; }
    @GetMapping("/csrf") public ApiResponse<?> csrf(CsrfToken token) {
        return ApiResponse.ok(Map.of("headerName",token.getHeaderName(),"token",token.getToken()));
    }
    @GetMapping("/me") public ApiResponse<SessionView> me() {
        var user = current.optional();
        return ApiResponse.ok(new SessionView(user != null, user));
    }
    public record SessionView(boolean authenticated, UserView user) { }
}
