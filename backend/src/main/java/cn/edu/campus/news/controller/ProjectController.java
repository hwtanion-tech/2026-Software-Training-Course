package cn.edu.campus.news.controller;

import cn.edu.campus.news.service.DatabaseProbe;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import cn.edu.campus.news.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api")
public class ProjectController {
    private final DatabaseProbe databaseProbe;
    private final String mode;

    public ProjectController(DatabaseProbe databaseProbe, @Value("${campus.mode}") String mode) {
        this.databaseProbe = databaseProbe;
        this.mode = mode;
    }

    @GetMapping("/health")
    public Health health() {
        return new Health("UP", "campus-news-backend", "week3", "后端服务运行正常");
    }

    @GetMapping("/health/database")
    public ResponseEntity<?> database() {
        var result = databaseProbe.check();
        return ResponseEntity.status("DOWN".equals(result.status())
                ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.OK).body(ApiResponse.ok(result));
    }

    @GetMapping("/project")
    public ApiResponse<?> project() {
        return ApiResponse.ok(Map.of("title","校园新闻及通知发布系统设计及实现", "stage","week3", "mode",mode,
                "features",List.of("登录鉴权","新闻及通知维护","发布撤回","查询分页","分类维护","账号管理与旧会话撤销"),
                "plannedFeatures",List.of("图片附件与富文本扩展","生产部署与性能优化")));
    }

    public record Health(String status, String service, String stage, String message) { }
}
