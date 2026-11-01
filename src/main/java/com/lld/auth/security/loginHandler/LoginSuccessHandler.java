package com.lld.auth.security.loginHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lld.auth.redis.lua.LuaScriptManager;
import com.lld.auth.security.entity.LoginLimit;
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
import com.lld.saltedfishutils.utils.PublicConstantKeys;
import com.lld.saltedfishutils.web.result.IpUtil;
import com.lld.saltedfishutils.web.result.ReturnResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
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
    //登陆日志工具
    private final LogUtil logUtil;
    //lua脚本管理
    private final LuaScriptManager luaScriptManager;

    private final StringRedisTemplate stringRedisTemplate;

    private static final String DATE_PATTERN = "yyyy-MM-dd";


    private final TokenUtil tokenUtil ;


    public LoginSuccessHandler(ObjectMapper objectMapper, RedisUtil redisUtil, SysUserService userService,
                               LogUtil logUtil, LuaScriptManager luaScriptManager, StringRedisTemplate stringRedisTemplate,
                               TokenUtil tokenUtil) {
        this.objectMapper = objectMapper;
        this.redisUtil = redisUtil;
        this.userService = userService;
        this.logUtil = logUtil;
        this.luaScriptManager = luaScriptManager;
        this.stringRedisTemplate = stringRedisTemplate;
        this.tokenUtil = tokenUtil;
    }


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        SysUser sysUser = (SysUser) authentication.getPrincipal();
        response.setContentType("application/json;charset=utf-8");
        String errorMessage = null;
        String logMessage = null;

        // 检查账户是否因为失败次数过多被锁定
        LoginLimit loginLimit = doRedisCheckAndLimitLua(sysUser.getUsername(), request);
        String status = loginLimit.getStatus();
        boolean isLocked = "locked".equals(status) || "iplocked".equals(status);
        if (isLocked) {

            String timeInfo = formatTime(loginLimit.getLockTtl());
            boolean isIpLocked = "iplocked".equals(status);
            String subject = isIpLocked ? "IP" : "账户";

            errorMessage = String.format("%s错误次数过多，请%s后重试", subject, timeInfo);
            logMessage = String.format("%s因多次登录失败已被锁定，%s后可重试", subject, timeInfo);


            //logUtil.logFailureLoginAsync(sysUser.getUsername(), IpUtil.getClientRealIp(request), "账密验证成功,但" + logMessage);

            try (ServletOutputStream outputStream = response.getOutputStream()) {
                String result = objectMapper.writeValueAsString(ReturnResult.error(errorMessage));
                outputStream.write(result.getBytes());
                outputStream.flush();
            }
            return;
        }


        sysUser.setPassWord(null);
        //更新数据库登录时间
        Date loginDate = new Date();
        sysUser.setLoginDate(loginDate);
        //这里有点蠢的，最好还是用消息中间件 ，用另一个程序处理，这里有点影响登录效率了
        userService.updateLoginDate(sysUser.getId(), loginDate);

        //创建token放入redis,更新登陆时间
        Map<String ,Object> customClaims = new HashMap<>();
        String authoritiesStr = sysUser.getAuthorities().stream().
                map(item -> item.getAuthority()).collect(Collectors.joining(","));
        customClaims.put("authorities",authoritiesStr);
        customClaims.put("test",1);
        Map<String, Object> claims = tokenUtil.generateAccessToken(sysUser.getId(), sysUser.getUsername(), customClaims);
        recordInRedis(claims, sysUser);

        //根据用户信息  生成  前端需要的用户数据
        WebUserInfo webUserInfo = MSUserMapper.INSTANCE.toWebUserInfo(sysUser);
        webUserInfo.setToken((String) claims.get("token"));
        webUserInfo.setUserName(sysUser.getUsername());

        //异步记录登陆日志
        //logUtil.logUserLoginAsync(sysUser.getId(), sysUser.getUsername(), IpUtil.getClientRealIp(request));

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
        List<String> keys = Arrays.asList(
                AuthPublicConstantKeys.USER_LOGIN_FAIL_LOCK_PREFIX + username,
                AuthPublicConstantKeys.USER_LOGIN_FAIL_COUNT_PREFIX  + username,
                AuthPublicConstantKeys.IP_LOGIN_FAIL_LOCK_PREFIX+ IpUtil.getClientRealIp(request),
                AuthPublicConstantKeys.IP_LOGIN_FAIL_COUNT_PREFIX+ IpUtil.getClientRealIp(request) // 这里有问题，请求走nginx转发的，ip地址获取不对
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
    private void recordInRedis(Map<String, Object> claims, SysUser sysUser) throws JsonProcessingException {
        // redis 用户token存放的key
        String redisTokenKey = PublicConstantKeys.Redis_User_Token_Prefix  + sysUser.getId().toString();
        // redis 中用户版本号
        String redisVersionKey = PublicConstantKeys.Redis_User_Version_Prefix + sysUser.getId().toString();
        // redis 记录当日 登陆过的用户 位图的key
        String todayOnlineBitmapKey = AuthPublicConstantKeys.USER_LOGGED_BITMAP_PREFIX+ DateUtil.formatDate(new Date(),DATE_PATTERN);

        SessionInfo sessionInfo = SessionInfo.builder()
                .jti(String.valueOf(claims.get("jti")))
                .userId(String.valueOf(sysUser.getId()))
                .userName(sysUser.getUsername())
                .loginTime(System.currentTimeMillis())
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
                String.valueOf(claims.get("jti")),
                sysUser.getVersion()==null?"0": sysUser.getVersion(),
                objectMapper.writeValueAsString(sessionInfo), //用户token的值
                String.valueOf(PublicConstantKeys.User_Token_Expire_Seconds), //用户token的过期时间
                String.valueOf(sysUser.getId())
        };

        RedisScript<List> loginRecordScript = luaScriptManager.getLoginScript();

        List<Object> result = (List<Object>) stringRedisTemplate.execute(
                loginRecordScript,
                keys, argv
        );

        System.out.println( result);


    }

}
