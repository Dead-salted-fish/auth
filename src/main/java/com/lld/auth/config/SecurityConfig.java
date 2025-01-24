package com.lld.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lld.auth.security.PasswordEncoder.CustomPasswordEncoderFactories;
import com.lld.auth.security.PasswordEncoder.RandomPasswordEncoder;
import com.lld.auth.security.filter.CustomUsernamePasswordAuthenticationFilter;
import com.lld.auth.security.filter.MyAuthenticationEntryPointFilter;
import com.lld.auth.security.filter.MyAuthenticationFilter;
import com.lld.auth.security.loginHandler.LoginFailureHandler;
import com.lld.auth.security.loginHandler.LoginSuccessHandler;
import com.lld.auth.security.userDetails.UserDetailsServiceImpl;
import com.lld.auth.utils.EncrytedRecordHelper;
import com.lld.saltedfishutils.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@DependsOn("multiConfigLoad")
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private LoginSuccessHandler loginSuccessHandler;

    @Autowired
    private LoginFailureHandler loginFailureHandler;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private MyAuthenticationEntryPointFilter  authenticationEntryPointFilter;

    @Autowired
    private EncrytedRecordHelper encrytedRecordHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private  RedisUtils redisUtils;
    //白名单
    private String[] URl_WHITELIST = {
            "/login",
            "/logout",
            "/hello",
            "/jx3/auth/user/register",
            "/auth/user/login",
            "/auth/user/getClientRsaPublicKey",
            "/jx3/auth/user/getMenus",
    };



    @Bean
    public PasswordEncoder passwordEncoder() {
        RandomPasswordEncoder randomPasswordEncoder = CustomPasswordEncoderFactories.createRandomPasswordEncoder();
        return randomPasswordEncoder;
    }


    @Bean
    public MyAuthenticationFilter myAuthenticationFilter() throws Exception {
        return new MyAuthenticationFilter(authenticationManager(), redisUtils,objectMapper);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable();
        //session禁用
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        //自定义 登录过滤器
        CustomUsernamePasswordAuthenticationFilter myAuthenticationFilter = new CustomUsernamePasswordAuthenticationFilter(super.authenticationManagerBean(), encrytedRecordHelper);
        myAuthenticationFilter.setAuthenticationSuccessHandler(loginSuccessHandler);
        myAuthenticationFilter.setAuthenticationFailureHandler(loginFailureHandler);
        http.addFilterAt(myAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        //登陆登出
         http.formLogin()
                 .loginPage("/auth/user/login")
                 .successHandler(loginSuccessHandler)
                 .failureHandler(loginFailureHandler);
        //异常处理
        http.exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPointFilter);


        //拦截规则
        http.authorizeRequests().antMatchers(URl_WHITELIST).permitAll()
                .anyRequest().authenticated();

        //自定义过滤配置
        http.addFilter(myAuthenticationFilter());
    }
}
