package com.lld.auth.security.loginHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lld.auth.security.entity.LoginLimit;
import com.lld.auth.security.entity.LoginLimitProperties;
import com.lld.auth.user.entity.SysUser;
import com.lld.auth.user.entity.WebUserInfo;
import com.lld.auth.user.mapstruct.MSUserMapper;
import com.lld.auth.user.service.SysUserService;
import com.lld.auth.utils.AuthPublicConstantKeys;
import com.lld.auth.utils.LogUtil;
import com.lld.saltedfishutils.crypto.jwt.TokenUtil;
import com.lld.saltedfishutils.date.DateUtil;
import com.lld.saltedfishutils.entity.SessionInfo;
import com.lld.saltedfishutils.redis.RedisUtil;
import com.lld.saltedfishutils.redis.lua.LuaScriptExecutor;
import com.lld.saltedfishutils.utils.PublicConstantKeys;
import com.lld.saltedfishutils.web.result.IpUtil;
import com.lld.saltedfishutils.web.result.ReturnResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    private final TokenUtil tokenUtil;

    //lua脚本执行器
    private LuaScriptExecutor luaScriptExecutor;
    //登录限制配置
    private LoginLimitProperties loginLimitProperties;


    public LoginSuccessHandler(ObjectMapper objectMapper, RedisUtil redisUtil,
                               SysUserService userService, TokenUtil tokenUtil,
                               LoginLimitProperties loginLimitProperties, LuaScriptExecutor luaScriptExecutor) {
        this.objectMapper = objectMapper;
        this.redisUtil = redisUtil;
        this.userService = userService;
        this.tokenUtil = tokenUtil;
        this.loginLimitProperties = loginLimitProperties;
        this.luaScriptExecutor = luaScriptExecutor;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        SysUser sysUser = (SysUser) authentication.getPrincipal();
        response.setContentType("application/json;charset=utf-8");

        // 检查账户是否因为失败次数过多被锁定
        String username = sysUser.getUsername();
        String remoteAddr = IpUtil.getClientRealIp(request);
        LoginLimit loginLimit = doRedisCheckAndLimitLua(username, request);
        String status = loginLimit.getStatus();

        // 最高优先级：Redis 限流锁定
        if (status.equals("locked") || status.equals("iplocked")) {
            String timeInfo = formatTime(loginLimit.getLockTtl());
            String subject = status.equals("iplocked") ? "IP" : "账户";
            String  errorMessage = String.format("%s错误次数过多，请%s后重试", subject, timeInfo);
            String  logMessage = String.format("%s因多次登录失败已被锁定，%s后可重试", subject, timeInfo);
            // 记录登录失败日志
            LogUtil.loginlogger.error("用户名:{},IP:{},登录失败,失败原因:{},剩余尝试次数:{}", username, remoteAddr, logMessage,loginLimit.getRemainingAttempts());
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                String result = objectMapper.writeValueAsString(ReturnResult.error(errorMessage));
                outputStream.write(result.getBytes());
                outputStream.flush();
            }
            return;
        }

        //更新数据库登录时间
        //这里有点蠢的，最好还是用消息中间件 ，用另一个程序处理，这里有点影响登录效率了
        Date loginDate = new Date();
        sysUser.setLoginDate(loginDate);
        userService.updateLoginDate(sysUser.getId(), loginDate);

        //创建token放入redis
        // 更新密码为null
        sysUser.setPassWord(null);
        Map<String, Object> customClaims = new HashMap<>();
        // 将权限转换为字符串
        String authoritiesStr = sysUser.getAuthorities().stream().
                map(item -> item.getAuthority()).collect(Collectors.joining(","));
        customClaims.put("authorities", authoritiesStr);
        Map<String, Object> claims = tokenUtil.generateAccessToken(sysUser.getId(), sysUser.getUsername(), customClaims);
        recordInRedis(claims, sysUser);

        //根据用户信息  生成  前端需要的用户数据
        WebUserInfo webUserInfo = MSUserMapper.INSTANCE.toWebUserInfo(sysUser);
        webUserInfo.setToken((String) claims.get("token"));
        webUserInfo.setUserName(sysUser.getUsername());

        //异步记录登陆日志
        LogUtil.loginlogger.info("用户名:{},IP:{},登录成功", sysUser.getUsername(), IpUtil.getClientRealIp(request));

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
                "true", //登录成功或者失败的标记
                loginLimitProperties.getAccountMaxFailures() + "",//用户登录最大失败次数
                loginLimitProperties.getLockDuration() + "",//锁定时间
                loginLimitProperties.getCountExpire() + "",//计数过期时间
                loginLimitProperties.getIpMaxFailures() + "",//IP登录最大失败次数
        };

        List<Object> redisReturn = luaScriptExecutor.execute("login_check_and_limit", List.class, keys, argv);
        return new LoginLimit(
                (String) redisReturn.get(0),
                ((Number) redisReturn.get(1)).longValue(),
                ((Number) redisReturn.get(2)).intValue(),
                (String) redisReturn.get(3)
        );
    }

    /**
     * 登录成功后redis记录信息
     */
    private void recordInRedis(Map<String, Object> claims, SysUser sysUser) throws JsonProcessingException {
        // redis 用户token存放的key
        String redisTokenKey = PublicConstantKeys.Redis_User_Token_Prefix + sysUser.getId();
        // redis 中用户版本号
        String redisVersionKey = PublicConstantKeys.Redis_User_Version_Prefix + sysUser.getId();
        // redis 记录当日 登陆过的用户 位图的key
        String todayOnlineBitmapKey = AuthPublicConstantKeys.USER_LOGGED_BITMAP_PREFIX + DateUtil.formatDate(new Date(), DATE_PATTERN);

        SessionInfo sessionInfo = SessionInfo.builder()
                .jti(String.valueOf(claims.get("jti")))
                .userId(String.valueOf(sysUser.getId()))
                .userName(sysUser.getUsername())
                .loginTime(sysUser.getLoginDate().getTime())
                .expireTime((Long) claims.get("expirationTime"))
                .version(sysUser.getVersion())
                .authority(sysUser.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(",")))
                .build();

        List<String> keys = Arrays.asList(
                redisTokenKey,
                redisVersionKey,//tokenKey  用户token
                AuthPublicConstantKeys.ONLINE_BITMAP_KEY,   //onlineBitmapKey 在线用户bitmap
                AuthPublicConstantKeys.ACTIVITY_ZSET_KEY,  //activityZsetKey 活跃用户zset  已经废弃，后面换其统计方式
                todayOnlineBitmapKey
        );

        // 修改 ARGV 为 List<String> 类型
        String[] argv = {
                String.valueOf(claims.get("jti")), //用户token的jit值
                sysUser.getVersion() == null ? "0" : sysUser.getVersion(),//用户版本号
                objectMapper.writeValueAsString(sessionInfo), //用户token的值
                String.valueOf(PublicConstantKeys.User_Token_Expire_Seconds), //用户token的过期时间
                String.valueOf(sysUser.getId()) //用户id
        };

        List<Object> result = luaScriptExecutor.execute("user_login", List.class, keys, argv);

        System.out.println(result);

    }

}
