package cn.edu.campus.news.service;

import cn.edu.campus.news.common.BusinessException;
import cn.edu.campus.news.domain.Models.UserView;
import cn.edu.campus.news.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUser {
    private final UserRepository users;
    public CurrentUser(UserRepository users) { this.users = users; }
    public UserView optional() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
        return users.byUsername(auth.getName()).filter(u -> u.enabled()).map(u -> u.view()).orElse(null);
    }
    public UserView required() {
        var user = optional();
        if (user == null) throw new BusinessException(401, "登录已失效，请重新登录");
        return user;
    }
    public UserView admin() {
        var user = required();
        if (!user.admin()) throw new BusinessException(403, "仅管理员可执行此操作");
        return user;
    }
}
