package com.lld.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lld.auth.security.PasswordEncoder.CustomPasswordEncoderFactories;
import com.lld.auth.security.PasswordEncoder.RandomPasswordEncoder;
import com.lld.auth.security.filter.CustomUsernamePasswordAuthenticationFilter;
import com.lld.auth.security.filter.MyAuthenticationEntryPointFilter;
import com.lld.auth.security.filter.TokenAuthenticationFilter;
import com.lld.auth.security.loginHandler.LoginFailureHandler;
import com.lld.auth.security.loginHandler.LoginSuccessHandler;
import com.lld.auth.security.userDetails.UserDetailsServiceImpl;
import com.lld.auth.utils.EncrytedRecordHelper;
import com.lld.saltedfishutils.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

//@DependsOn("multiConfigLoad")
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    @Lazy
    private LoginSuccessHandler loginSuccessHandler;

    @Autowired
    private LoginFailureHandler loginFailureHandler;


    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private MyAuthenticationEntryPointFilter  authenticationEntryPointFilter;

    @Autowired
    private EncrytedRecordHelper encrytedRecordHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    @Lazy
    public void setUserDetailsService(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
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
    public AuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false); // 关键配置
        return provider;
    }



    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() throws Exception {
        return new TokenAuthenticationFilter(authenticationManager(), redisUtil,objectMapper);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {

//        auth.userDetailsService(userDetailsService);

        auth.authenticationProvider(daoAuthenticationProvider());


    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        // 禁用默认表单登录
        http.formLogin().disable();
        // 启用 CORS（跨域资源共享）支持，禁用 CSRF（跨站请求伪造）保护机制
        http.cors().and().csrf().disable();
        // 禁用 HTTP Basic
        http.httpBasic().disable();
        //// 禁用默认注销端点
        http.logout().disable();
        // 禁用默认的会话管理
        http.sessionManagement().disable();
        // 禁用记住我
        http.rememberMe().disable();
        // 不能禁用匿名访问，否则白名单失效，FilterSecurityInterceptor 需要一个用户验证权限，匿名访问在没有用户的时候创建一个匿名用户
//        http.anonymous().disable();


//        //session禁用
//        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        //自定义 登录过滤器
        CustomUsernamePasswordAuthenticationFilter myAuthenticationFilter = new CustomUsernamePasswordAuthenticationFilter(super.authenticationManagerBean(), encrytedRecordHelper);
        myAuthenticationFilter.setAuthenticationSuccessHandler(loginSuccessHandler);
        myAuthenticationFilter.setAuthenticationFailureHandler(loginFailureHandler);
        http.addFilterAt(myAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

//        //登陆登出
//         http.formLogin()
//                 .loginPage("/auth/user/login")
//                 .successHandler(loginSuccessHandler)
//                 .failureHandler(loginFailureHandler);
        //登陆登出
        http.formLogin().disable();



        //token处理以及异常处理
        http.addFilterBefore(tokenAuthenticationFilter(), BasicAuthenticationFilter.class)
                .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPointFilter);


        //拦截规则
        http.authorizeRequests().antMatchers(URl_WHITELIST).permitAll()
                .anyRequest().authenticated();

        //自定义过滤配置
//        http.addFilter(tokenAuthenticationFilter());
    }
}

//小巧思 ，通过迭代直接移除security 的验证filter,达到没有security的效果
//    @Bean
//    public ApplicationRunner filterOrderLogger() {
//        FilterChainProxy filterChainProxy =   applicationContext.getBean(
//                "springSecurityFilterChain", FilterChainProxy.class);
//
//        // 获取 filterChains 字段
//        Field filterChainsField = null;
//        try {
//            filterChainsField = filterChainProxy.getClass().getDeclaredField("filterChains");
//        } catch (NoSuchFieldException e) {
//            throw new RuntimeException(e);
//        }
//
//        // 设置字段可访问
//        filterChainsField.setAccessible(true);
//
//        // 获取字段值
//        @SuppressWarnings("unchecked")
//        List<SecurityFilterChain> filterChains =
//                null;
//        try {
//            filterChains = (List<SecurityFilterChain>) filterChainsField.get(filterChainProxy);
//            for (SecurityFilterChain filterChain : filterChains) {
//                List<Filter> filters = filterChain.getFilters();
//                System.out.println(filters.getClass().getName());
//                Iterator<Filter> iterator = filters.iterator();
//                while (iterator.hasNext()){
//                    iterator.next() ;
//                    iterator.remove();
//                }
//                System.out.println("filters: " + filters);
//            }
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        }
//
//
//        return  null;
//    }