package com.lld.auth.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lld.saltedfishutils.web.result.ReturnResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;


import java.io.IOException;
@Component
public class MyAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private ObjectMapper objectMapper;
    public MyAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        System.out.println("进入 MyAuthenticationEntryPointFilter ");
        System.out.println("===== commence 被调用 =====");
        System.out.println("  URI              = " + request.getRequestURI());
        System.out.println("  DispatcherType   = " + request.getDispatcherType());
        System.out.println("  error.request_uri= " + request.getAttribute("jakarta.servlet.error.request_uri"));
        System.out.println("  error.exception  = " + request.getAttribute("jakarta.servlet.error.exception"));
        System.out.println("  异常类型          = " + authException.getClass().getName());

        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        ServletOutputStream outputStream = response.getOutputStream();

         //获取自定义的错误信息
        String errorMsg = (String) request.getAttribute("AUTH_ERROR_MSG");
        if(errorMsg == null){
            errorMsg = authException.getMessage() ;
        }


        String result = objectMapper.writeValueAsString(ReturnResult.error(response.SC_UNAUTHORIZED, errorMsg));
        outputStream.write(result.getBytes());
        outputStream.flush();
        outputStream.close();
    }

}
