package com.example.calorie.controller;

import com.example.calorie.common.BusinessException;
import com.example.calorie.common.GlobalExceptionHandler;
import com.example.calorie.common.ResultCode;
import com.example.calorie.service.AuthService;
import com.example.calorie.vo.UserVO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {
    private final AuthService service = mock(AuthService.class);
    private MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    private static final String VALID = "{\"username\":\"test_user\",\"password\":\"valid-password\"}";

    @Test void validRegistrationReturnsUser() throws Exception {
        var user = new UserVO(); user.setId(7L); user.setUsername("test_user");
        when(service.register(any())).thenReturn(user);
        mvc().perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("test_user"));
    }
    @Test void invalidRegistrationNeverWritesUser() throws Exception {
        mvc().perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ab\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void duplicateUsernameIsActionable() throws Exception {
        when(service.register(any())).thenThrow(new BusinessException(ResultCode.CONFLICT,"用户名已被占用"));
        mvc().perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("用户名已被占用"));
    }
    @Test void unavailableDatabaseExplainsRegistrationAndLoginFailure() throws Exception {
        when(service.register(any())).thenThrow(new CannotCreateTransactionException("private DB detail"));
        when(service.login(any())).thenThrow(new CannotGetJdbcConnectionException("private DB detail"));
        var mvc=mvc();
        for(var path:new String[]{"register","login"}) {
            mvc.perform(post("/api/v1/auth/"+path).contentType(MediaType.APPLICATION_JSON).content(VALID))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("数据库暂未连接，注册和登录暂不可用，请启动 MySQL 后重试"));
        }
    }
    @Test void healthReportsDatabaseDownWithoutExposingConnectionDetails() throws Exception {
        var jdbc=mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(),eq(String.class))).thenThrow(new CannotGetJdbcConnectionException("private DB detail"));
        MockMvcBuilders.standaloneSetup(new HealthController(jdbc)).build()
                .perform(get("/api/v1/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DB_DOWN"))
                .andExpect(jsonPath("$.data.dbError").doesNotExist());
    }
}
