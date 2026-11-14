package com.lld.auth.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lld.auth.security.entity.MyUsernamePasswordAuthenticationToken;
import com.lld.auth.security.entity.AuthWhitelist;
import com.lld.auth.user.service.SysUserService;
import com.lld.saltedfishutils.crypto.jwt.TokenValidator;
import com.lld.saltedfishutils.entity.SessionInfo;
import com.lld.saltedfishutils.exception.TokenInvalidException;
import com.lld.saltedfishutils.redis.RedisUtil;
import com.lld.saltedfishutils.web.result.ErrorResponseWriter;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private RedisUtil redisUtil;

    private ObjectMapper objectMapper;

    private AuthWhitelist authWhitelist;

    private TokenValidator tokenValidator;

    private SysUserService sysUserService;

    public TokenAuthenticationFilter(AuthenticationManager authticarionManager,
                                     RedisUtil redisUtil,
                                     ObjectMapper objectMapper,
                                     AuthWhitelist authWhitelist,
                                     TokenValidator tokenValidator,
                                     SysUserService sysUserService
                                     ) {
        this.redisUtil = redisUtil;
        this.objectMapper = objectMapper;
        this.authWhitelist = authWhitelist;
        this.tokenValidator = tokenValidator;
        this.sysUserService = sysUserService;

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            // 获取 token
            String requestURI = request.getRequestURI();
            String REFRESH_URL = "/auth/user/refreshToken";
            String ACCESS_URL = "/auth/user/accessToken";
            String token = null;
            if(requestURI.equals(REFRESH_URL)||requestURI.equals(ACCESS_URL)){
                token = request.getHeader("refreshToken");
            }else {
                token = request.getHeader("token");
            }

            System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "headerToken ===== :" + token);
            System.out.println("request.getRequestURI() ===== :" + request.getRequestURI());
            if (authWhitelist.isAnonymous(requestURI)) {
                // 匿名路径无论带不带 token 都放行，不解析、不校验
                chain.doFilter(request, response);
                return;
            }
            if (!StringUtils.hasText(token)) {
                throw new TokenInvalidException("token为空");
            }

            SessionInfo sessionInfo = null;
            try {
                sessionInfo = tokenValidator.validToken( token, request.getRequestURI());

            } catch (ExpiredJwtException e) {
                throw new TokenInvalidException("登录已过期，请重新登录", e);
            } catch (JwtException | IllegalArgumentException e) {
                throw new TokenInvalidException("无效的登录凭证", e);
            } catch (TokenInvalidException e) {
                throw new TokenInvalidException("token无效", e);
            }

            String authorities = sysUserService.getUserAuthorities(sessionInfo.getUserId());
            List<SimpleGrantedAuthority> list =  Arrays.stream(authorities
                            .split(","))
                    .map(item->  new SimpleGrantedAuthority( item))
                    .toList();

            MyUsernamePasswordAuthenticationToken auth = new MyUsernamePasswordAuthenticationToken(
                    sessionInfo.getUserName(), null, list, Long.valueOf(sessionInfo.getUserId()));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            ErrorResponseWriter.write(request, response, objectMapper, e);
            return;
        }

        chain.doFilter(request, response);

    }


}
