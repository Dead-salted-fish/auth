package com.lld.auth.security.loginHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lld.auth.redis.lua.LuaScriptManager;
import com.lld.auth.security.entity.LoginLimit;
import com.lld.auth.utils.LoginLogUtil;
import com.lld.auth.utils.AuthPublicConstantKeys;
import com.lld.saltedfishutils.web.result.ReturnResult;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 登陆失败处理
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {


    private final ObjectMapper objectMapper;
    //登陆日志工具
    private final LoginLogUtil loginLogUtil;

    //lua脚本管理
    private final LuaScriptManager luaScriptManager;

    private final StringRedisTemplate stringRedisTemplate;

    public LoginFailureHandler(ObjectMapper objectMapper, LoginLogUtil loginLogUtil, LuaScriptManager luaScriptManager,
                               StringRedisTemplate stringRedisTemplate) {
        this.objectMapper = objectMapper;
        this.loginLogUtil = loginLogUtil;
        this.luaScriptManager = luaScriptManager;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        response.setContentType("application/json;charset=utf-8");

        String username = (String) request.getAttribute("fromWebUsername");
        String remoteAddr = request.getRemoteAddr();
        String errorMessage = null;
        String logMessage = null;

        // 执行Lua脚本，看失败后是否限制 ，失败超过三次会被限制登录
        LoginLimit loginLimit = doRedisCheckAndLimitLua(username);
        String status = loginLimit.getStatus();


        // 检查账号是否被锁定
        boolean isLocked = "locked".equals(status);
        if (isLocked) {
            Long lockTtl = loginLimit.getLockTtl();
            String timeInfo = formatTime(lockTtl);
            errorMessage = "登录次数过多，请" + timeInfo + "后重试";
            logMessage = "账户因多次登录失败已被锁定，" + timeInfo + "后可重试";
        }

        // 根据异常类型设置错误信息
        if (exception instanceof UsernameNotFoundException) {
            errorMessage = errorMessage == null ? "用户名或密码不正确，请重新输入" : errorMessage;
            logMessage = appendLogMessage(logMessage, "没有此用户");
        } else if (exception instanceof BadCredentialsException) {
            errorMessage = errorMessage == null ? "用户名或密码不正确，请重新输入" : errorMessage;
            logMessage = appendLogMessage(logMessage, "密码错误");
        } else if (exception instanceof LockedException) {
            errorMessage = errorMessage == null ? "账户已被锁定，请联系管理员" : errorMessage;
            logMessage = appendLogMessage(logMessage, "账户已被锁定，请联系管理员");
        } else {
            errorMessage = errorMessage == null ? exception.getMessage(): errorMessage;
            logMessage = appendLogMessage(logMessage, errorMessage);;
        }


        // 记录登录失败日志
        loginLogUtil.logFailureLoginAsync(username, remoteAddr, logMessage);

        String result = objectMapper.writeValueAsString(ReturnResult.error(errorMessage));
        // 使用try-with-resources自动管理资源
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            outputStream.write(result.getBytes());
            outputStream.flush();
        }
    }

    // 格式化时间显示
    private String formatTime(Long lockTtl) {
        if (lockTtl == null) return "稍后";

        long minutes = lockTtl / 60;
        long seconds = lockTtl % 60;

        StringBuilder sb = new StringBuilder();
        if (minutes > 0) {
            sb.append(minutes).append("分");
        }
        if (seconds > 0) {
            sb.append(seconds).append("秒");
        }
        return sb.length() > 0 ? sb.toString() : "稍后";
    }

    // 组合日志信息
    private String appendLogMessage(String existingMessage, String newMessage) {
        if (existingMessage == null) {
            return newMessage;
        }
        return newMessage + "，" + existingMessage;
    }

    /**
     * 登录失败时，记录失败次数，检查账户是否因为登录过多被锁定
     * 登录成功时，检查是否因为登录失败次数过多而被锁定
     * **/
    private LoginLimit doRedisCheckAndLimitLua(String username) {
        List<String> keys = Arrays.asList(
                AuthPublicConstantKeys.USER_LOGIN_FAIL_LOCK_PREFIX  + username,
                AuthPublicConstantKeys.USER_LOGIN_FAIL_COUNT_PREFIX + username
        );

        // 修改 ARGV 为 List<String> 类型
        String[] argv = {
                "false", //登录成功或者失败的标记
                "3", //失败次数
                "600",//锁定时间
                "180"//计数过期时间
        };

        RedisScript<List> loginCheckAndLimitScript = luaScriptManager.getLoginCheckAndLimitScript();


        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) stringRedisTemplate.execute(
                loginCheckAndLimitScript,
                keys, argv
        );
        return new LoginLimit((String) result.get(0), ((Number) result.get(1)).longValue(), ((Number) result.get(2)).intValue(), (String) result.get(3));
    }
}
