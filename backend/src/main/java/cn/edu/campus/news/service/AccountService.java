package cn.edu.campus.news.service;

import cn.edu.campus.news.common.BusinessException;
import cn.edu.campus.news.domain.Models.*;
import cn.edu.campus.news.repository.UserRepository;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.nio.charset.StandardCharsets;

@Service
public class AccountService {
    private final UserRepository users;
    private final CurrentUser current;
    private final PasswordEncoder encoder;
    private final SessionRegistry sessions;
    public AccountService(UserRepository users,CurrentUser current,PasswordEncoder encoder,SessionRegistry sessions) {
        this.users=users;this.current=current;this.encoder=encoder;this.sessions=sessions;
    }
    public Page<AccountView> list(String keyword,int page,int size) {
        current.admin();
        if(page<1 || page>1000000 || size<1 || size>50 || keyword.length()>200)
            throw new BusinessException(400,"请检查关键词、页码及每页条数");
        return users.list(keyword.strip(),page,size);
    }
    @Transactional
    public AccountView create(AccountCreate input) {
        current.admin();checkPassword(input.password());users.lockAccounts();
        if(users.byUsername(input.username()).isPresent()) throw new BusinessException(409,"账号已存在");
        String hash=encoder.encode(input.password());
        users.create(input.username(),hash,input.displayName().strip(),input.role());
        var account=users.byUsername(input.username()).orElseThrow();
        if(!input.enabled()) users.update(account.id(),account.displayName(),account.role(),false,hash);
        return view(users.byId(account.id()).orElseThrow());
    }
    @Transactional
    public AccountView update(long id,AccountUpdate input) {
        var actor=current.admin();users.lockAccounts();
        var account=users.byId(id).orElseThrow(() -> new BusinessException(404,"账号不存在"));
        if(actor.id()==id && (!input.enabled() || !"ADMIN".equals(input.role())))
            throw new BusinessException(409,"不能停用自己或修改自己的管理员角色");
        if(account.enabled() && "ADMIN".equals(account.role()) && (!input.enabled() || !"ADMIN".equals(input.role()))
                && users.enabledAdmins()<=1) throw new BusinessException(409,"必须保留至少一个启用的管理员");
        boolean reset=input.password()!=null && !input.password().isEmpty();
        if(reset) checkPassword(input.password());
        String hash=reset?encoder.encode(input.password()):account.passwordHash();
        users.update(id,input.displayName().strip(),input.role(),input.enabled(),hash);
        // 数据提交成功才撤销旧会话；重设密码、改角色或启停后均要求重新登录。
        if(reset || !account.role().equals(input.role()) || account.enabled()!=input.enabled()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { expire(account.username()); }
            });
        }
        return view(users.byId(id).orElseThrow());
    }
    private void checkPassword(String password) {
        if(password==null || password.isBlank() || password.length()<12 || password.length()>64
                || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new BusinessException(400,"密码须为12至64位且UTF-8编码不超过72字节");
    }
    private void expire(String username) {
        for(Object principal:sessions.getAllPrincipals()) {
            if(principal instanceof UserDetails details && username.equals(details.getUsername()))
                sessions.getAllSessions(principal,false).forEach(session -> session.expireNow());
        }
    }
    private AccountView view(UserAccount value) {
        return new AccountView(value.id(),value.username(),value.displayName(),value.role(),value.enabled());
    }
}
