package com.lld.auth.security.loginHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lld.auth.security.entity.LoginLimit;
import com.lld.auth.security.entity.LoginLimitProperties;
import com.lld.auth.utils.AuthPublicConstantKeys;
import com.lld.auth.utils.LogUtil;
import com.lld.saltedfishutils.redis.lua.LuaScriptExecutor;
import com.lld.saltedfishutils.web.result.IpUtil;
import com.lld.saltedfishutils.web.result.ReturnResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 登陆失败处理
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {


    private final ObjectMapper objectMapper;

    //lua脚本执行器
    private LuaScriptExecutor luaScriptExecutor;

    private LoginLimitProperties loginLimitProperties;

    public LoginFailureHandler(ObjectMapper objectMapper, LuaScriptExecutor luaScriptExecutor,
                               LoginLimitProperties loginLimitProperties) {
        this.objectMapper = objectMapper;
        this.luaScriptExecutor = luaScriptExecutor;
        this.loginLimitProperties = loginLimitProperties;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        ;
        //设置返回状态
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        //从 AuthenticationException 中获取用户名
        Authentication authenticationRequest = exception.getAuthenticationRequest();
        String username = (String) authenticationRequest.getPrincipal();
        String remoteAddr = IpUtil.getClientRealIp(request);

        // 执行Lua脚本，看失败后是否限制 ，失败超过指定次数会被限制登录
        LoginLimit loginLimit = doRedisCheckAndLimitLua(username, request);
        Map<String, String> msg = resolveMessage(loginLimit, exception);

        // 获取错误信息与日志信息
        String errorMessage = msg.get("errorMessage");
        String logMessage = msg.get("logMessage");


        // 记录登录失败日志
        LogUtil.loginlogger.error("用户名:{},IP:{},登录失败,失败原因:{},剩余尝试次数:{}", username, remoteAddr, logMessage,loginLimit.getRemainingAttempts());

        String result = objectMapper.writeValueAsString(ReturnResult.error(errorMessage));
        // 使用try-with-resources自动管理资源
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            outputStream.write(result.getBytes());
            outputStream.flush();
        }
    }



    // 根据 Redis 锁定状态 / 异常类型，返回 errorMessage 与 logMessage
    private Map<String, String> resolveMessage(LoginLimit loginLimit, AuthenticationException exception) {
        String status = loginLimit.getStatus();

        // 最高优先级：Redis 限流锁定
        if (status.equals("locked") || status.equals("iplocked")) {
            String timeInfo = formatTime(loginLimit.getLockTtl());
            String subject = status.equals("iplocked") ? "IP" : "账户";
            String error = String.format("%s错误次数过多，请%s后重试", subject, timeInfo);
            String log = String.format("%s因多次登录失败已被锁定，%s后可重试", subject, timeInfo);
            return Map.of("errorMessage", error, "logMessage", log);
        }

        String logMessage;
        if (exception instanceof UsernameNotFoundException) {
            logMessage = "没有此用户";
        } else if (exception instanceof BadCredentialsException) {
            logMessage = "密码错误";
        } else if (exception instanceof LockedException) {
            logMessage = "账户已被锁定，请联系管理员";
        } else {
            logMessage = exception.getMessage();
        }

        // 如果是账号被锁定，则返回前端的错误直接显示原因
        boolean credentialError = exception instanceof UsernameNotFoundException
                || exception instanceof BadCredentialsException;
        String error = credentialError ? "用户名或密码不正确，请重新输入" : logMessage;
        return Map.of("errorMessage", error, "logMessage", logMessage);
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
     *
     **/
    private LoginLimit doRedisCheckAndLimitLua(String username, HttpServletRequest request) {
        String clientRealIp = IpUtil.getClientRealIp(request);
        List<String> keys = Arrays.asList(
                AuthPublicConstantKeys.USER_LOGIN_FAIL_LOCK_PREFIX + username,
                AuthPublicConstantKeys.USER_LOGIN_FAIL_COUNT_PREFIX + username,
                AuthPublicConstantKeys.IP_LOGIN_FAIL_LOCK_PREFIX + clientRealIp,
                AuthPublicConstantKeys.IP_LOGIN_FAIL_COUNT_PREFIX + clientRealIp
        );

        // 修改 ARGV 为 String[] 类型
        String[] argv = {
                "false", //登录成功或者失败的标记
                loginLimitProperties.getAccountMaxFailures()+"",//用户登录最大失败次数
                loginLimitProperties.getLockDuration()+"",//锁定时间
                loginLimitProperties.getCountExpire()+"",//计数过期时间
                loginLimitProperties.getIpMaxFailures()+"",//IP登录最大失败次数
        };

        List<Object> redisReturn = luaScriptExecutor.execute("login_check_and_limit", List.class, keys, argv);


        return new LoginLimit(
                (String) redisReturn.get(0),
                ((Number) redisReturn.get(1)).longValue(),
                ((Number) redisReturn.get(2)).intValue(),
                (String) redisReturn.get(3)
        );
    }
}
