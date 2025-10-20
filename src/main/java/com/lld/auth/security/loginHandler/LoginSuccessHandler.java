package com.lld.auth.security.loginHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lld.auth.redis.lua.LuaScriptManager;
import com.lld.auth.security.entity.LoginLimit;
import com.lld.auth.user.entity.SysUser;
import com.lld.auth.user.entity.WebUserInfo;
import com.lld.auth.user.mapstruct.MSUserMapper;
import com.lld.auth.user.service.SysUserService;
import com.lld.auth.utils.LoginLogUtil;
import com.lld.auth.utils.AuthPublicConstantKeys;
import com.lld.auth.utils.TokenGenerator;
import com.lld.saltedfishutils.utils.PublicConstantKeys;
import com.lld.saltedfishutils.redis.RedisUtil;
import com.lld.saltedfishutils.web.result.ReturnResult;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 登陆成功处理
 */
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    //fastjosn
    private final ObjectMapper objectMapper;
    //redis工具
    private final RedisUtil redisUtil;
    //用户服务
    private SysUserService userService;
    //登陆日志工具
    private final LoginLogUtil loginLogUtil;
    //lua脚本管理
    private final LuaScriptManager luaScriptManager;

    private final StringRedisTemplate stringRedisTemplate;

    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);


    public LoginSuccessHandler(ObjectMapper objectMapper, RedisUtil redisUtil, SysUserService userService,
                               LoginLogUtil loginLogUtil, LuaScriptManager luaScriptManager, StringRedisTemplate stringRedisTemplate) {
        this.objectMapper = objectMapper;
        this.redisUtil = redisUtil;
        this.userService = userService;
        this.loginLogUtil = loginLogUtil;
        this.luaScriptManager = luaScriptManager;
        this.stringRedisTemplate = stringRedisTemplate;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        SysUser sysUser = (SysUser) authentication.getPrincipal();
        response.setContentType("application/json;charset=utf-8");

        // 检查账户是否因为失败次数过多被锁定
        LoginLimit loginLimit = doRedisCheckAndLimitLua(sysUser.getUsername());
        String status = loginLimit.getStatus();
        if ("locked".equals(status)) {
            //被锁
            Long lockTtl = loginLimit.getLockTtl();
            String timeInfo = formatTime(lockTtl);
            String errmessage = "登录次数过多，请" + timeInfo + "后重试";
            String logMessage = "账户因多次登录失败已被锁定，" + timeInfo + "后可重试";

            loginLogUtil.logFailureLoginAsync(sysUser.getUsername(), getClientIpAddress(request), "账密验证成功,但" + logMessage);

            try (ServletOutputStream outputStream = response.getOutputStream()) {
                String result = objectMapper.writeValueAsString(ReturnResult.error(errmessage));
                outputStream.write(result.getBytes());
                outputStream.flush();
            }
            return;
        }


        sysUser.setPassWord(null);
        Date loginDate = new Date();
        sysUser.setLoginDate(loginDate);
        //更新数据库登录时间
        //这里有点蠢的，最好还是用消息中间件 ，用另一个程序处理，这里有点影响登录效率了
        userService.updateLoginDate(sysUser.getId(), loginDate);

        //创建token放入redis,更新登陆时间
        String token = TokenGenerator.generateToken();
        recordInRedis(token, sysUser);

        //根据用户信息  生成  前端需要的用户数据
        WebUserInfo webUserInfo = MSUserMapper.INSTANCE.toWebUserInfo(sysUser);
        webUserInfo.setToken(token);
        webUserInfo.setUserName(sysUser.getUsername());

        //异步记录登陆日志
        loginLogUtil.logUserLoginAsync(sysUser.getId(), sysUser.getUsername(), getClientIpAddress(request));

        try (ServletOutputStream outputStream = response.getOutputStream()) {
            String result = objectMapper.writeValueAsString(ReturnResult.OK(webUserInfo));
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


    /**
     * 登录失败时，记录失败次数，检查账户是否因为登录过多被锁定
     * 登录成功时，检查是否因为登录失败次数过多而被锁定
     **/
    private LoginLimit doRedisCheckAndLimitLua(String username) {
        List<String> keys = Arrays.asList(
                AuthPublicConstantKeys.USER_LOGIN_FAIL_LOCK_PREFIX + username,
                AuthPublicConstantKeys.USER_LOGIN_FAIL_COUNT_PREFIX  + username
        );

        // 修改 ARGV 为 List<String> 类型
        String[] argv = {
                "true", //登录成功或者失败的标记
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

    /**
     * 登录成功后redis记录信息
     */
    private void recordInRedis(String token, SysUser sysUser) throws JsonProcessingException {
        String redisTokenKey = PublicConstantKeys.Redis_User_Token_Prefix  + token;
        String todayOnlineBitmapKey = AuthPublicConstantKeys.TODAY_ONLINE_BITMAP_PREFIX+LocalDate.now().format(DATE_FORMATTER);
        List<String> keys = Arrays.asList(
                redisTokenKey, //tokenKey  用户token
                AuthPublicConstantKeys.ONLINE_BITMAP_KEY,   //onlineBitmapKey 在线用户bitmap
                AuthPublicConstantKeys.ACTIVITY_ZSET_KEY,  //activityZsetKey 活跃用户zset
                todayOnlineBitmapKey
        );

        // 修改 ARGV 为 List<String> 类型
        String[] argv = {
                String.valueOf(objectMapper.writeValueAsString(sysUser)), //用户token的值
                String.valueOf(PublicConstantKeys.User_Token_Expire_Seconds), //用户token的过期时间
                String.valueOf(sysUser.getId()), //用户id
                String.valueOf(System.currentTimeMillis()),

        };

        RedisScript<List> loginRecordScript = luaScriptManager.getLoginScript();

        List<Object> result = (List<Object>) stringRedisTemplate.execute(
                loginRecordScript,
                keys, argv
        );

        System.out.println( result);


    }
   /**
    *  获取客户端ip
    * **/
    private String getClientIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
