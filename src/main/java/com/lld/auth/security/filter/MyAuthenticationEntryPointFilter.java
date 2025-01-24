package com.lld.auth.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lld.saltedfishutils.utils.ReturnResult;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
@Component
public class MyAuthenticationEntryPointFilter implements AuthenticationEntryPoint {

    private ObjectMapper objectMapper;
    public MyAuthenticationEntryPointFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setContentType("application/json;charset=utf-8");
        ServletOutputStream outputStream = response.getOutputStream();
        String result = objectMapper.writeValueAsString(ReturnResult.error(response.SC_UNAUTHORIZED, authException.getMessage()));
        outputStream.write(result.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
