package cn.edu.campus.news;

import cn.edu.campus.news.service.DatabaseProbe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "campus.database.enabled=false")
@AutoConfigureMockMvc
class ProjectApiTest {
    @Autowired MockMvc mvc;

    @Test
    void applicationStartsWithoutDatabase() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void disabledDatabaseMustNotBeReportedAsConnected() throws Exception {
        mvc.perform(get("/api/health/database"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_CONFIGURED"));
    }

    @Test
    void projectDistinguishesPlannedFeaturesFromWeekOneDeliverables() throws Exception {
        mvc.perform(get("/api/project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(3))
                .andExpect(jsonPath("$.plannedFeatures[0]").value("登录与权限验证"));
    }

    @Test
    void databaseFailureDoesNotExposeCredentials() {
        var probe = new DatabaseProbe(true, "jdbc:unsupported:example", "private-user", "secret");
        var result = probe.check();
        assertEquals("DOWN", result.status());
        assertEquals("MySQL 检查失败，请核对服务、数据库名及本地凭据。", result.message());
    }
}
