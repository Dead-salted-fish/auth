package com.lld.auth.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lld.auth.security.entity.MyUsernamePasswordAuthenticationToken;
import com.lld.auth.user.entity.SysUser;
import com.lld.saltedfishutils.utils.RedisUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

public class MyAuthenticationFilter extends BasicAuthenticationFilter {
    private RedisUtils redisUtils;

    private ObjectMapper objectMapper;

    public MyAuthenticationFilter(AuthenticationManager authticarionManager, RedisUtils redisUtil,ObjectMapper objectMapper) {
        super(authticarionManager);
        this.redisUtils = redisUtil;
        this.objectMapper = objectMapper;

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException, ServletException {
        String token = request.getHeader("token");
        if (token == null){
            chain.doFilter(request, response);
            return;
        }
        if (!redisUtils.exists(token)){
            throw new RuntimeException("token已失效");
        }

        SysUser sysUser = objectMapper.convertValue(redisUtils.get(token), SysUser.class);
        MyUsernamePasswordAuthenticationToken myAuthenticationToken = new MyUsernamePasswordAuthenticationToken(sysUser.getUsername(), null, new ArrayList<>(),sysUser.getId());
        SecurityContextHolder.getContext().setAuthentication(myAuthenticationToken);
        chain.doFilter(request, response);


    }
}
