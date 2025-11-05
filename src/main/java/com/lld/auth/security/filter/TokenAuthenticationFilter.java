package com.lld.auth.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lld.auth.security.entity.MyUsernamePasswordAuthenticationToken;
import com.lld.auth.user.entity.SysUser;
import com.lld.saltedfishutils.redis.RedisUtil;
import com.lld.saltedfishutils.utils.PublicConstantKeys;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class TokenAuthenticationFilter extends BasicAuthenticationFilter {
    private RedisUtil redisUtil;

    private ObjectMapper objectMapper;

    public TokenAuthenticationFilter(AuthenticationManager authticarionManager, RedisUtil redisUtil, ObjectMapper objectMapper) {
        super(authticarionManager);
        this.redisUtil = redisUtil;
        this.objectMapper = objectMapper;

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException, ServletException {
        String headerToken = request.getHeader("token");
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"headerToken ===== :" + headerToken);
        if (headerToken == null) {
            chain.doFilter(request, response);
            return;
        }

        String redisUserTokenKey = PublicConstantKeys.Redis_User_Token_Prefix + headerToken;
        if (!redisUtil.exists(redisUserTokenKey)) {
            // 关键修改：抛出带明确消息的异常
            request.setAttribute("AUTH_ERROR_MSG", "token已失效");
            throw new BadCredentialsException("token已失效");
        }

        SysUser sysUser = objectMapper.convertValue(redisUtil.get(redisUserTokenKey), SysUser.class);
        MyUsernamePasswordAuthenticationToken auth = new MyUsernamePasswordAuthenticationToken(
                sysUser.getUsername(), null, new ArrayList<>(), sysUser.getId());
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);

    }
}
