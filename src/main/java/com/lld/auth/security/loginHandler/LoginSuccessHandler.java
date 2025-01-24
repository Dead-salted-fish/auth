package com.lld.auth.security.loginHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lld.auth.user.entity.SysUser;
import com.lld.auth.user.entity.WebUserInfo;
import com.lld.auth.user.mapstruct.MSUserMapper;
import com.lld.auth.user.service.SysUserService;
import com.lld.auth.utils.*;
import com.lld.saltedfishutils.utils.RedisUtils;
import com.lld.saltedfishutils.utils.ReturnResult;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 登陆成功处理
 */
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    //fastjosn
    private final ObjectMapper objectMapper;
    //redis工具
    private final RedisUtils redisUtils;
    //用户服务
    private SysUserService userService;
    //登陆日志工具
    private final LoginLogUtil loginLogUtil;

    public LoginSuccessHandler(ObjectMapper objectMapper, RedisUtils redisUtils, SysUserService userService, LoginLogUtil loginLogUtil) {
        this.objectMapper = objectMapper;
        this.redisUtils = redisUtils;
        this.userService = userService;
        this.loginLogUtil = loginLogUtil;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {


        //创建token放入redis,更新登陆时间
        String token = TokenGenerator.generateToken();
        SysUser sysUser = (SysUser) authentication.getPrincipal();
        sysUser.setPassWord(null);
        Date loginDate = new Date();
        sysUser.setLoginDate(loginDate);
        userService.updateLoginDate(sysUser.getId(), loginDate);
        redisUtils.set(token, sysUser, 60 * 60 * 24, TimeUnit.SECONDS);

        //异步记录登陆日志
        loginLogUtil.logUserLoginAsync(sysUser.getId(),sysUser.getUsername(), getClientIpAddress(request));

        //根据 用户信息  生成  前端需要的  用户数据
        WebUserInfo webUserInfo = MSUserMapper.INSTANCE.toWebUserInfo(sysUser);
        webUserInfo.setToken(token);
        webUserInfo.setUserName(sysUser.getUsername());
        //返回给前端
        response.setContentType("application/json;charset=utf-8");
        ServletOutputStream outputStream = response.getOutputStream();
        String result = objectMapper.writeValueAsString(ReturnResult.OK(webUserInfo));
        outputStream.write(result.getBytes());
        outputStream.flush();
        outputStream.close();
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
