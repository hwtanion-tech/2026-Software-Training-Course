package cn.edu.campus.news;

import cn.edu.campus.news.domain.Models.*;
import cn.edu.campus.news.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:week2-tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "campus.demo-seed=false", "campus.bootstrap.enabled=false"
})
@ActiveProfiles("demo")
@AutoConfigureMockMvc
class ProjectApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired ContentRepository contents;
    @Autowired PasswordEncoder encoder;
    @Autowired org.springframework.security.core.session.SessionRegistry sessionRegistry;
    long editorId, otherId, categoryId, ownNewsId, otherNewsId;

    @BeforeEach void setup() {
        for(Object principal:sessionRegistry.getAllPrincipals())
            for(var session:sessionRegistry.getAllSessions(principal,true)) sessionRegistry.removeSessionInformation(session.getSessionId());
        jdbc.update("DELETE FROM news"); jdbc.update("DELETE FROM notice");
        jdbc.update("DELETE FROM news_category"); jdbc.update("DELETE FROM sys_user");
        users.create("editor",encoder.encode("TestEditor!2026"),"编辑","PUBLISHER");
        users.create("other",encoder.encode("TestOther!2026"),"其他编辑","PUBLISHER");
        users.create("admin",encoder.encode("TestAdmin!2026"),"管理员","ADMIN");
        users.create("disabled",encoder.encode("TestDisabled!2026"),"停用用户","PUBLISHER");
        jdbc.update("UPDATE sys_user SET enabled=FALSE WHERE username='disabled'");
        editorId=users.byUsername("editor").orElseThrow().id(); otherId=users.byUsername("other").orElseThrow().id();
        categories.create(new CategoryInput("校园动态",1,true)); categoryId=categories.all(true).getFirst().id();
        ownNewsId=contents.create(Kind.NEWS,input("自己的草稿"),editorId);
        otherNewsId=contents.create(Kind.NEWS,input("别人的草稿"),otherId);
    }
    ContentInput input(String title) { return new ContentInput(title,"摘要","正文内容",categoryId,"编辑部",null); }
    MockHttpSession login(String username,String password) throws Exception {
        var result=mvc.perform(post("/api/auth/login").with(csrf()).param("username",username).param("password",password))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession)result.getRequest().getSession(false);
    }
    MockHttpSession editor() throws Exception { return login("editor","TestEditor!2026"); }
    ResultActions send(MockHttpServletRequestBuilder builder, MockHttpSession session, Object body) throws Exception {
        if(body!=null) builder.contentType("application/json").content(mapper.writeValueAsString(body));
        return mvc.perform(builder.session(session).with(csrf()));
    }
    void publish(long id,MockHttpSession session) throws Exception { send(post("/api/manage/news/"+id+"/publish"),session,null).andExpect(status().isOk()); }

    @Test void healthStartsWithEmbeddedDatabase() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.stage").value("week3"));
        send(get("/api/health/database"),login("admin","TestAdmin!2026"),null)
                .andExpect(jsonPath("$.data.status").value("UP")).andExpect(jsonPath("$.data.mode").value("demo"));
    }
    @Test void passwordIsHashedAndNeverReturned() throws Exception {
        assertNotEquals("TestEditor!2026",users.byUsername("editor").orElseThrow().passwordHash());
        send(get("/api/auth/me"),editor(),null).andExpect(jsonPath("$.data.user.username").value("editor"))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());
    }
    @Test void wrongPasswordAndDisabledAccountFail() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).param("username","editor").param("password","wrong"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).param("username","disabled").param("password","TestDisabled!2026"))
                .andExpect(status().isUnauthorized());
    }
    @Test void sessionSurvivesRequestsAndLogoutInvalidatesIt() throws Exception {
        var session=editor();
        mvc.perform(get("/api/auth/me").session(session)).andExpect(jsonPath("$.data.authenticated").value(true));
        send(post("/api/auth/logout"),session,null).andExpect(status().isOk());
        assertTrue(session.isInvalid());
        mvc.perform(get("/api/manage/news")).andExpect(status().isUnauthorized());
    }
    @Test void missingCsrfBlocksLoginAndWrites() throws Exception {
        mvc.perform(post("/api/auth/login").param("username","editor").param("password","TestEditor!2026"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/manage/news").session(editor()).contentType("application/json").content(mapper.writeValueAsString(input("新建"))))
                .andExpect(status().isForbidden());
    }
    @Test void actualCsrfEndpointSupportsLoginAndSubsequentWrite() throws Exception {
        var tokenResult=mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var session=(MockHttpSession)tokenResult.getRequest().getSession(false);
        JsonNode csrfValue=mapper.readTree(tokenResult.getResponse().getContentAsString()).get("data");
        mvc.perform(post("/api/auth/login").session(session).header(csrfValue.get("headerName").asText(),csrfValue.get("token").asText())
                .param("username","editor").param("password","TestEditor!2026")).andExpect(status().isOk());
        var refreshed=mvc.perform(get("/api/auth/csrf").session(session)).andReturn();
        var value=mapper.readTree(refreshed.getResponse().getContentAsString()).get("data");
        mvc.perform(post("/api/manage/news").session(session).header(value.get("headerName").asText(),value.get("token").asText())
                .contentType("application/json").content(mapper.writeValueAsString(input("真实令牌测试")))).andExpect(status().isOk());
    }
    @Test void privateListAndDetailRejectVisitors() throws Exception {
        mvc.perform(get("/api/manage/news")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/manage/notices/1")).andExpect(status().isUnauthorized());
    }
    @Test void ownerIsolationAndAdminOverride() throws Exception {
        var editor=editor();
        send(get("/api/manage/news"),editor,null).andExpect(jsonPath("$.data.total").value(1));
        send(get("/api/manage/news/"+otherNewsId),editor,null).andExpect(status().isForbidden());
        send(put("/api/manage/news/"+otherNewsId),editor,input("越权编辑")).andExpect(status().isForbidden());
        send(post("/api/manage/news/"+otherNewsId+"/publish"),editor,null).andExpect(status().isForbidden());
        send(delete("/api/manage/news/"+otherNewsId),editor,null).andExpect(status().isForbidden());
        send(get("/api/manage/news"),login("admin","TestAdmin!2026"),null).andExpect(jsonPath("$.data.total").value(2));
    }
    @Test void creatorCannotBeForged() throws Exception {
        var data=mapper.convertValue(input("伪造创建者"),Map.class);
        data.put("creatorId",otherId); data.put("status","PUBLISHED");
        send(post("/api/manage/news"),editor(),data).andExpect(jsonPath("$.data.creatorId").value(editorId))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }
    @Test void publishWithdrawAndRepublishControlPublicVisibility() throws Exception {
        var session=editor();
        mvc.perform(get("/api/public/news/"+ownNewsId)).andExpect(status().isNotFound());
        publish(ownNewsId,session);
        mvc.perform(get("/api/public/news/"+ownNewsId)).andExpect(status().isOk()).andExpect(jsonPath("$.data.publishedAt").isNotEmpty());
        send(post("/api/manage/news/"+ownNewsId+"/withdraw"),session,null).andExpect(status().isOk());
        mvc.perform(get("/api/public/news/"+ownNewsId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/public/news")).andExpect(jsonPath("$.data.total").value(0));
        publish(ownNewsId,session);
        mvc.perform(get("/api/public/news")).andExpect(jsonPath("$.data.total").value(1));
    }
    @Test void editingPublishedContentReturnsToDraft() throws Exception {
        var session=editor(); publish(ownNewsId,session);
        send(put("/api/manage/news/"+ownNewsId),session,input("修改后的标题"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
        mvc.perform(get("/api/public/news/"+ownNewsId)).andExpect(status().isNotFound());
    }
    @Test void invalidTransitionsAndPublishedDeletionAreRejected() throws Exception {
        var session=editor();
        send(post("/api/manage/news/"+ownNewsId+"/withdraw"),session,null).andExpect(status().isConflict());
        publish(ownNewsId,session);
        send(post("/api/manage/news/"+ownNewsId+"/publish"),session,null).andExpect(status().isConflict());
        send(delete("/api/manage/news/"+ownNewsId),session,null).andExpect(status().isConflict());
        send(post("/api/manage/news/"+ownNewsId+"/withdraw"),session,null).andExpect(status().isOk());
        send(delete("/api/manage/news/"+ownNewsId),session,null).andExpect(status().isOk());
        send(get("/api/manage/news/"+ownNewsId),session,null).andExpect(status().isNotFound());
    }
    @Test void blankAndOversizedInputAreRejected() throws Exception {
        var session=editor();
        send(post("/api/manage/news"),session,input("  ")).andExpect(status().isBadRequest());
        send(post("/api/manage/news"),session,input("字".repeat(201))).andExpect(status().isBadRequest());
        send(post("/api/manage/news"),session,new ContentInput("标题",null,"  ",categoryId,"编辑",null)).andExpect(status().isBadRequest());
    }
    @Test void nonexistentAndDisabledCategoriesAreRejected() throws Exception {
        var session=editor();
        send(post("/api/manage/news"),session,new ContentInput("标题",null,"正文",999999L,"作者",null)).andExpect(status().isBadRequest());
        categories.update(categoryId,new CategoryInput("校园动态",1,false));
        send(post("/api/manage/news"),session,input("停用分类")).andExpect(status().isBadRequest());
        send(post("/api/manage/news/"+ownNewsId+"/publish"),session,null).andExpect(status().isBadRequest());
    }
    @Test void keywordCategoryAndPaginationWorkTogether() throws Exception {
        var session=editor();
        for(int i=0;i<5;i++) { long id=contents.create(Kind.NEWS,input("搜索样例"+i),editorId);publish(id,session); }
        mvc.perform(get("/api/public/news").param("keyword","搜索").param("categoryId",String.valueOf(categoryId)).param("size","2").param("page","2"))
                .andExpect(jsonPath("$.data.total").value(5)).andExpect(jsonPath("$.data.pages").value(3)).andExpect(jsonPath("$.data.items.length()").value(2));
        mvc.perform(get("/api/public/news").param("keyword","不存在的文字")).andExpect(jsonPath("$.data.total").value(0));
    }
    @Test void literalWildcardsAndInjectionCannotExpandSearch() throws Exception {
        var session=editor();long id=contents.create(Kind.NEWS,input("完成100%任务"),editorId);publish(id,session);publish(ownNewsId,session);
        mvc.perform(get("/api/public/news").param("keyword","%" )).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/public/news").param("keyword","' OR 1=1 --")).andExpect(jsonPath("$.data.total").value(0));
    }
    @Test void invalidPageAndStatusAreRejected() throws Exception {
        mvc.perform(get("/api/public/news").param("page","0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/public/news").param("size","51")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/public/news").param("page","abc")).andExpect(status().isBadRequest());
        send(get("/api/manage/news").param("status","ALL"),editor(),null).andExpect(status().isBadRequest());
    }
    @Test void noticeHasSameOwnershipAndLifecycleRules() throws Exception {
        var session=editor();
        var result=send(post("/api/manage/notices"),session,new ContentInput("通知标题",null,"通知正文",null,null,"实验中心"))
                .andExpect(status().isOk()).andReturn();
        long id=mapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
        send(post("/api/manage/notices/"+id+"/publish"),session,null).andExpect(status().isOk());
        mvc.perform(get("/api/public/notices/"+id)).andExpect(jsonPath("$.data.publisherUnit").value("实验中心"));
        send(put("/api/manage/notices/"+id),login("other","TestOther!2026"),new ContentInput("越权",null,"正文",null,null,"单位"))
                .andExpect(status().isForbidden());
        send(post("/api/manage/notices/"+id+"/withdraw"),session,null).andExpect(status().isOk());
        mvc.perform(get("/api/public/notices/"+id)).andExpect(status().isNotFound());
    }
    @Test void categoriesRequireAdminAndRejectDuplicateNames() throws Exception {
        send(post("/api/admin/categories"),editor(),new CategoryInput("新分类",2,true)).andExpect(status().isForbidden());
        var admin=login("admin","TestAdmin!2026");
        send(post("/api/admin/categories"),admin,new CategoryInput("新分类",2,true)).andExpect(status().isOk());
        send(post("/api/admin/categories"),admin,new CategoryInput("新分类",2,true)).andExpect(status().isConflict());
        send(put("/api/admin/categories/"+categoryId),admin,new CategoryInput("已停用",1,false)).andExpect(status().isOk());
    }
    @Test void accountManagementRequiresAdminAndCsrf() throws Exception {
        mvc.perform(get("/api/admin/accounts")).andExpect(status().isUnauthorized());
        send(get("/api/admin/accounts"),editor(),null).andExpect(status().isForbidden());
        var admin=login("admin","TestAdmin!2026");
        mvc.perform(post("/api/admin/accounts").session(admin).contentType("application/json")
                .content(mapper.writeValueAsString(new AccountCreate("new_editor","编辑","PUBLISHER",true,"NewEditor!2026"))))
                .andExpect(status().isForbidden());
    }
    @Test void adminCreatesAccountAndListsWithoutSecrets() throws Exception {
        var admin=login("admin","TestAdmin!2026");
        send(post("/api/admin/accounts"),admin,new AccountCreate("new_editor","新增编辑","PUBLISHER",true,"NewEditor!2026"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.username").value("new_editor"))
                .andExpect(jsonPath("$.data.password").doesNotExist()).andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        assertTrue(encoder.matches("NewEditor!2026",users.byUsername("new_editor").orElseThrow().passwordHash()));
        send(get("/api/admin/accounts").param("keyword","新增").param("size","1"),admin,null)
                .andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.items[0].enabled").value(true))
                .andExpect(jsonPath("$.data.items[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/auth/me").session(login("new_editor","NewEditor!2026")))
                .andExpect(jsonPath("$.data.authenticated").value(true));
    }
    @Test void duplicateUsernameAndInvalidAccountFieldsFail() throws Exception {
        var admin=login("admin","TestAdmin!2026");
        send(post("/api/admin/accounts"),admin,new AccountCreate("editor","编辑","PUBLISHER",true,"NewEditor!2026")).andExpect(status().isConflict());
        send(post("/api/admin/accounts"),admin,new AccountCreate("不合法账号","编辑","PUBLISHER",true,"NewEditor!2026")).andExpect(status().isBadRequest());
        send(post("/api/admin/accounts"),admin,new AccountCreate("valid_user","  ","PUBLISHER",true,"NewEditor!2026")).andExpect(status().isBadRequest());
        send(post("/api/admin/accounts"),admin,new AccountCreate("valid_user","编辑","ROOT",true,"NewEditor!2026")).andExpect(status().isBadRequest());
        send(post("/api/admin/accounts"),admin,new AccountCreate("valid_user","编辑","PUBLISHER",true,"short")).andExpect(status().isBadRequest());
        send(post("/api/admin/accounts"),admin,new AccountCreate("valid_user","编辑","PUBLISHER",true,"密".repeat(25))).andExpect(status().isBadRequest());
        send(get("/api/admin/accounts").param("size","51"),admin,null).andExpect(status().isBadRequest());
    }
    @Test void disabledAccountSessionExpiresAndReenableCannotReuseIt() throws Exception {
        var old=editor(); var admin=login("admin","TestAdmin!2026");
        send(put("/api/admin/accounts/"+editorId),admin,new AccountUpdate("编辑","PUBLISHER",false,""))
                .andExpect(status().isOk());
        // 先重新启用，再尝试旧会话，验证不是仅在停用期间检查 enabled。
        send(put("/api/admin/accounts/"+editorId),admin,new AccountUpdate("编辑","PUBLISHER",true,""))
                .andExpect(status().isOk());
        mvc.perform(get("/api/manage/news").session(old)).andExpect(status().isUnauthorized());
        assertTrue(old.isInvalid());
        mvc.perform(get("/api/manage/news").session(editor())).andExpect(status().isOk());
    }
    @Test void roleDowngradeRevokesAdminSession() throws Exception {
        users.create("admin_two",encoder.encode("AdminTwo!2026"),"第二管理员","ADMIN");
        long id=users.byUsername("admin_two").orElseThrow().id();
        var target=login("admin_two","AdminTwo!2026");
        send(put("/api/admin/accounts/"+id),login("admin","TestAdmin!2026"),new AccountUpdate("第二管理员","PUBLISHER",true,null))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/accounts").session(target)).andExpect(status().isUnauthorized());
        send(get("/api/admin/accounts"),login("admin_two","AdminTwo!2026"),null).andExpect(status().isForbidden());
    }
    @Test void resetPasswordRevokesSessionAndOldPasswordFails() throws Exception {
        var old=editor();
        send(put("/api/admin/accounts/"+editorId),login("admin","TestAdmin!2026"),
                new AccountUpdate("新名称","PUBLISHER",true,"ChangedEditor!2026")).andExpect(status().isOk());
        mvc.perform(get("/api/manage/news").session(old)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).param("username","editor").param("password","TestEditor!2026"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").session(login("editor","ChangedEditor!2026")))
                .andExpect(jsonPath("$.data.user.displayName").value("新名称"));
    }
    @Test void adminCannotDisableOrDemoteSelf() throws Exception {
        var admin=login("admin","TestAdmin!2026"); long id=users.byUsername("admin").orElseThrow().id();
        send(put("/api/admin/accounts/"+id),admin,new AccountUpdate("管理员","ADMIN",false,null)).andExpect(status().isConflict());
        send(put("/api/admin/accounts/"+id),admin,new AccountUpdate("管理员","PUBLISHER",true,null)).andExpect(status().isConflict());
        assertEquals(1,users.enabledAdmins());
    }
    @Test void profileEditPreservesPasswordSessionAndContentReferences() throws Exception {
        var session=editor(); String hash=users.byId(editorId).orElseThrow().passwordHash();
        send(put("/api/admin/accounts/"+editorId),login("admin","TestAdmin!2026"),new AccountUpdate("更新名称","PUBLISHER",true,""))
                .andExpect(status().isOk());
        assertEquals(hash,users.byId(editorId).orElseThrow().passwordHash());
        mvc.perform(get("/api/manage/news/"+ownNewsId).session(session)).andExpect(jsonPath("$.data.creatorName").value("更新名称"));
        send(put("/api/admin/accounts/9999999"),login("admin","TestAdmin!2026"),new AccountUpdate("名称","PUBLISHER",true,null))
                .andExpect(status().isNotFound());
    }
    @Test void htmlContentIsReturnedAsPlainTextAndMalformedJsonIsRejected() throws Exception {
        String text="<script>alert('x')</script>\n第二行"; var session=editor();
        var response=send(post("/api/manage/news"),session,new ContentInput("纯文本测试",null,text,categoryId,"作者",null))
                .andExpect(status().isOk()).andReturn();
        long id=mapper.readTree(response.getResponse().getContentAsString()).path("data").path("id").asLong();
        publish(id,session);
        mvc.perform(get("/api/public/news/"+id)).andExpect(jsonPath("$.data.body").value(text));
        mvc.perform(post("/api/manage/news").session(session).with(csrf()).contentType("application/json").content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/public/news/9999999")).andExpect(status().isNotFound());
    }
}
