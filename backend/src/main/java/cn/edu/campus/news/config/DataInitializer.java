package cn.edu.campus.news.config;

import cn.edu.campus.news.domain.Models.*;
import cn.edu.campus.news.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/** 演示数据只在空的demo库初始化；MySQL账号须显式启用bootstrap。 */
@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final CategoryRepository categories;
    private final ContentRepository contents;
    private final PasswordEncoder encoder;
    private final boolean demo, bootstrap;
    private final String mode, username, password;
    public DataInitializer(UserRepository users,CategoryRepository categories,ContentRepository contents,PasswordEncoder encoder,
            @Value("${campus.demo-seed}") boolean demo,@Value("${campus.mode}") String mode,
            @Value("${campus.bootstrap.enabled}") boolean bootstrap,@Value("${campus.bootstrap.username}") String username,
            @Value("${campus.bootstrap.password}") String password) {
        this.users=users;this.categories=categories;this.contents=contents;this.encoder=encoder;
        this.demo=demo;this.mode=mode;this.bootstrap=bootstrap;this.username=username;this.password=password;
    }
    @Override @Transactional
    public void run(String... args) {
        if (users.count()!=0) return;
        if (demo && "demo".equals(mode)) {
            users.create("admin",encoder.encode("DemoAdmin!2026"),"管理员","ADMIN");
            users.create("editor",encoder.encode("DemoEditor!2026"),"校园编辑","PUBLISHER");
            users.create("other",encoder.encode("DemoOther!2026"),"另一位编辑","PUBLISHER");
            seed();
        } else if (bootstrap) {
            if (!username.matches("[A-Za-z0-9_]{3,50}") || password.isBlank() || password.length()<12 || password.length()>64
                    || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
                throw new IllegalStateException("初始化账号须为3至50位字母数字下划线，密码须为12至64位且UTF-8编码不超过72字节");
            users.create(username,encoder.encode(password),"管理员","ADMIN");
        }
    }
    private void seed() {
        for (var name : new String[]{"校园动态","学术活动","学生生活"}) {
            if (categories.all(false).stream().noneMatch(c -> c.name().equals(name)))
                categories.create(new CategoryInput(name, categories.all(false).size()+1,true));
        }
        var categoryList=categories.all(true);
        long editor=users.byUsername("editor").orElseThrow().id();
        long other=users.byUsername("other").orElseThrow().id();
        String[] titles={"校园科技文化节正式启动","人工智能公开课开放报名","图书馆读书分享会圆满举行",
                "软件开发实践作品交流会","青年志愿者服务活动纪实","跨学科创新论坛走进校园",
                "秋季运动会精彩瞬间","数据库技术专题讲座回顾","校园摄影展征集活动开始",
                "开源软件社团举办技术沙龙","学生团队完成课程项目展示","实验中心开放创新实践空间"};
        for (int i=0;i<titles.length;i++) {
            var id=contents.create(Kind.NEWS,new ContentInput(titles[i],"记录校园里的学习、交流与成长。此内容为课程验收样例。",
                    "校园新闻中心讯："+titles[i]+"。\n\n本条内容用于课程项目演示。你可以登录后台修改标题与正文，体验发布、撤回和分类检索。\n\n实际活动时间与地点以学校正式通知为准。",
                    categoryList.get(i%categoryList.size()).id(),"校园编辑部",null),i%3==0?other:editor);
            contents.status(Kind.NEWS,id,"PUBLISHED",LocalDateTime.of(2026,10,27,10,0).minusDays(i));
        }
        contents.create(Kind.NEWS,new ContentInput("尚未发布的校园草稿","这条草稿仅所属编辑和管理员可见","草稿内容不应被公开接口返回。",
                categoryList.get(0).id(),"校园编辑部",null),editor);
        for(int i=0;i<4;i++) {
            long id=contents.create(Kind.NOTICE,new ContentInput(new String[]{"关于实验室开放时间的通知","关于实践课程材料提交的通知","关于校园讲座报名的通知","关于图书馆借阅安排的通知"}[i],null,
                    "各位同学：\n请关注相关事项并做好安排。本通知为实训演示样例，不作为实际办事依据。\n\n如有问题，请联系对应发布单位。",null,null,"教务办公室"),editor);
            contents.status(Kind.NOTICE,id,i==3?"WITHDRAWN":"PUBLISHED",LocalDateTime.of(2026,10,27,9,0).minusDays(i));
        }
    }
}
