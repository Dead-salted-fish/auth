package com.lld.auth.config;

import com.lld.auth.security.PasswordEncoder.CustomPasswordEncoderFactories;
import com.lld.auth.security.PasswordEncoder.RandomPasswordEncoder;
import com.lld.auth.security.loginHandler.LoginFailureHandler;
import com.lld.auth.security.loginHandler.LoginSuccessHandler;
import com.lld.auth.security.userDetails.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private LoginSuccessHandler loginSuccessHandler;

    @Autowired
    private LoginFailureHandler loginFailureHandler;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    private String[] URl_WHITELIST = {
            "/login",
            "/logout",
            "/hello",
            "/jx3/auth/user/register",
            "/jx3/auth/user/login",
            "/jx3/auth/user/getClientRsaPublicKey",
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        RandomPasswordEncoder randomPasswordEncoder = CustomPasswordEncoderFactories.createRandomPasswordEncoder();
        return randomPasswordEncoder;
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
        //登陆登出
         http.formLogin()
                 .loginPage("/jx3/auth/user/login")
                 .successHandler(loginSuccessHandler)
                 .failureHandler(loginFailureHandler);

        //拦截规则
        http.authorizeRequests().antMatchers(URl_WHITELIST).permitAll()
                .anyRequest().authenticated();
    }
}
