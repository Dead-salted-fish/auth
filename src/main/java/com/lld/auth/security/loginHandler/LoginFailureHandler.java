package com.lld.auth.security.loginHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lld.saltedfishutils.utils.ReturnResult;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
/**
 * 登陆失败处理
 * */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {


    private final ObjectMapper objectMapper;

    public LoginFailureHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        response.setContentType("application/json;charset=utf-8");


        ServletOutputStream outputStream = response.getOutputStream();

        String result = objectMapper.writeValueAsString(ReturnResult.error(exception.getMessage()));
        outputStream.write(result.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
