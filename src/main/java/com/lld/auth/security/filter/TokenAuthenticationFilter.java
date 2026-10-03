package com.lld.auth.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lld.auth.security.entity.MyUsernamePasswordAuthenticationToken;
import com.lld.saltedfishutils.analyzetoken.AnalyzeTokenUtil;
import com.lld.saltedfishutils.crypto.jwt.TokenUtil;
import com.lld.saltedfishutils.entity.SessionInfo;
import com.lld.saltedfishutils.redis.RedisUtil;
import com.lld.saltedfishutils.redis.lua.LuaScriptExecutor;
import com.lld.saltedfishutils.web.result.ErrorResponseWriter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import com.lld.saltedfishutils.exception.TokenInvalidException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private RedisUtil redisUtil;

    private ObjectMapper objectMapper;

    private TokenUtil tokenUtil;

    private LuaScriptExecutor luaScriptExecutor;


    public TokenAuthenticationFilter(AuthenticationManager authticarionManager,
                                     RedisUtil redisUtil,
                                     ObjectMapper objectMapper,
                                     TokenUtil tokenUtil,
                                     LuaScriptExecutor luaScriptExecutor
                                    ) {
        this.redisUtil = redisUtil;
        this.tokenUtil = tokenUtil;
        this.luaScriptExecutor = luaScriptExecutor;

    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
       try{
           String headerToken = request.getHeader("token");
           System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"headerToken ===== :" + headerToken);
           System.out.println("request.getRequestURI() ===== :" + request.getRequestURI());
           if (headerToken == null) {
               chain.doFilter(request, response);
               return;
           }

           SessionInfo sessionInfo = null;
           try{
               sessionInfo =  AnalyzeTokenUtil.validToken(tokenUtil, headerToken, luaScriptExecutor);
           }catch (ExpiredJwtException e) {
               throw new TokenInvalidException("登录已过期，请重新登录", e);
           } catch (JwtException | IllegalArgumentException e) {
               throw new TokenInvalidException("无效的登录凭证", e);
           } catch (TokenInvalidException e) {
               throw new TokenInvalidException("token无效", e);
           }
           List<SimpleGrantedAuthority> list =  Arrays.stream(sessionInfo.getJwtAuthority()
                   .split(","))
                   .map(item->  new SimpleGrantedAuthority( item))
                   .toList();

           MyUsernamePasswordAuthenticationToken auth = new MyUsernamePasswordAuthenticationToken(
                   sessionInfo.getUserName(), null,list, Long.valueOf(sessionInfo.getUserId()));
           SecurityContextHolder.getContext().setAuthentication(auth);
       }catch (Exception e){
           SecurityContextHolder.clearContext();
           ErrorResponseWriter.write(request,response, objectMapper,e);
           return;
       }

        chain.doFilter(request, response);

    }
}
