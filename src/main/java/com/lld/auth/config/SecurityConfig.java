package com.lld.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lld.auth.security.PasswordEncoder.CustomPasswordEncoderFactories;
import com.lld.auth.security.filter.CustomUsernamePasswordAuthenticationFilter;
import com.lld.auth.security.filter.MyAuthenticationEntryPoint;
import com.lld.auth.security.filter.TokenAuthenticationFilter;
import com.lld.auth.security.loginHandler.LoginFailureHandler;
import com.lld.auth.security.loginHandler.LoginSuccessHandler;
import com.lld.auth.security.userDetails.UserDetailsServiceImpl;
import com.lld.auth.utils.EncrytedRecordHelper;
import com.lld.saltedfishutils.crypto.jwt.TokenUtil;
import com.lld.saltedfishutils.redis.RedisUtil;
import com.lld.saltedfishutils.redis.lua.LuaScriptExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

//@DependsOn("multiConfigLoad")
@Configuration
@EnableWebSecurity // 5.7+ 推荐显式声明
@EnableMethodSecurity(jsr250Enabled = true)
public class SecurityConfig {

    @Autowired
    @Lazy
    private LoginSuccessHandler loginSuccessHandler;

    @Autowired
    private LoginFailureHandler loginFailureHandler;


    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private MyAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private EncrytedRecordHelper encrytedRecordHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private LuaScriptExecutor luaScriptExecutor;


    @Autowired
    @Lazy
    public void setUserDetailsService(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    // 白名单
    private static final String[] URL_WHITELIST = {
            "/login",
            "/logout",
            "/hello",
            "/auth/user/register",
            "/auth/user/login",
            "/auth/user/getClientRsaPublicKey",
            "/auth/user/getMenus",
    };




    @Bean
    public PasswordEncoder passwordEncoder() {
        // 使用 Spring Security 5.7+ 推荐的 DelegatingPasswordEncoder
        return CustomPasswordEncoderFactories.createRandomPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false); // 关键配置
        return provider;
    }

    /**
     * 5.7+ 标准暴露 AuthenticationManager 的方式
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Spring Security 5.7+ 核心安全过滤器链配置 (替代 WebSecurityConfigurerAdapter)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {

        // 禁用默认表单登录
        http.formLogin(form -> form.disable());
        // 启用 CORS（跨域资源共享）支持，禁用 CSRF（跨站请求伪造）保护机制
        http.cors(cors -> {
        }).csrf(csrf -> csrf.disable());
        // 禁用 HTTP Basic
        http.httpBasic(basic -> basic.disable());
        // 禁用默认注销端点
        http.logout(logout -> logout.disable());
        // 5.7 推荐：使用 STATELESS 策略，不创建 Session，避免 SecurityContext 在多线程环境下的传递问题
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        // 禁用记住我
        http.rememberMe(rememberMe -> rememberMe.disable());

        // 自定义登录过滤器
        CustomUsernamePasswordAuthenticationFilter myAuthenticationFilter = new CustomUsernamePasswordAuthenticationFilter(authenticationManager, encrytedRecordHelper);
        myAuthenticationFilter.setAuthenticationSuccessHandler(loginSuccessHandler);
        myAuthenticationFilter.setAuthenticationFailureHandler(loginFailureHandler);
        http.addFilterAt(myAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Token 认证过滤器
        TokenAuthenticationFilter tokenAuthenticationFilter = new TokenAuthenticationFilter(authenticationManager,
                redisUtil,
                objectMapper,
                tokenUtil,
                luaScriptExecutor
                );

        // Token 处理以及异常处理
        http.addFilterBefore(tokenAuthenticationFilter, BasicAuthenticationFilter.class);
        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)

        );



        // 拦截规则 (5.7 已废弃 authorizeRequests/antMatchers，改为 authorizeHttpRequests/requestMatchers)
        // 注意 这里的拦截 和 @PreAuthorize 注解 是 相互独立的，
        // @PreAuthorize 注解 是 基于方法的 权限控制， 这里是基于 url 的 权限控制
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(URL_WHITELIST).permitAll()
                .anyRequest().authenticated()
        );

        return http.build();
    }

    // 小巧思：通过迭代直接移除 security 的验证 filter，达到没有 security 的效果
    // @Bean
    // public ApplicationRunner filterOrderLogger(ApplicationContext applicationContext) {
    //     FilterChainProxy filterChainProxy = applicationContext.getBean(
    //             "springSecurityFilterChain", FilterChainProxy.class);
    //
    //     // 获取 filterChains 字段
    //     Field filterChainsField = null;
    //     try {
    //         filterChainsField = filterChainProxy.getClass().getDeclaredField("filterChains");
    //     } catch (NoSuchFieldException e) {
    //         throw new RuntimeException(e);
    //     }
    //
    //     // 设置字段可访问
    //     filterChainsField.setAccessible(true);
    //
    //     // 获取字段值
    //     @SuppressWarnings("unchecked")
    //     List<SecurityFilterChain> filterChains = null;
    //     try {
    //         filterChains = (List<SecurityFilterChain>) filterChainsField.get(filterChainProxy);
    //         for (SecurityFilterChain filterChain : filterChains) {
    //             List<Filter> filters = filterChain.getFilters();
    //             System.out.println(filters.getClass().getName());
    //             Iterator<Filter> iterator = filters.iterator();
    //             while (iterator.hasNext()){
    //                 iterator.next();
    //                 iterator.remove();
    //             }
    //             System.out.println("filters: " + filters);
    //         }
    //     } catch (IllegalAccessException e) {
    //         throw new RuntimeException(e);
    //     }
    //
    //     return null;
    // }
}
