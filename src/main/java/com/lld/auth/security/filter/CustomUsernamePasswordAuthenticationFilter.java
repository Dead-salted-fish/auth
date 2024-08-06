package com.lld.auth.security.filter;

import com.lld.auth.user.entity.EncryptedRecords;
import com.lld.auth.utils.EncrytedRecordHelper;
import com.lld.saltedfishutils.utils.AESUtils;
import com.lld.saltedfishutils.utils.RsaUtil;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 这里就是照抄 UsernamePasswordAuthenticationFilter，做了一点点修改
 * */
public class CustomUsernamePasswordAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    public static final String SPRING_SECURITY_FORM_USERNAME_KEY = "username";

    public static final String SPRING_SECURITY_FORM_PASSWORD_KEY = "password";

    private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER = new AntPathRequestMatcher("/jx3/auth/user/login",
            "POST");

    private String usernameParameter = SPRING_SECURITY_FORM_USERNAME_KEY;

    private String passwordParameter = SPRING_SECURITY_FORM_PASSWORD_KEY;

    private String webAesKeyParameter = "encryptedAesKey";

    private String clientRsaIdParameter = "clientRsa";

    private boolean postOnly = true;

    private EncrytedRecordHelper encrytedRecordHelper;
    public CustomUsernamePasswordAuthenticationFilter() {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER);
    }

    public CustomUsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
    }

    public CustomUsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager,EncrytedRecordHelper encrytedRecordHelper) {
        super(DEFAULT_ANT_PATH_REQUEST_MATCHER, authenticationManager);
        this.encrytedRecordHelper = encrytedRecordHelper;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        if (this.postOnly && !request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException("认证不支持: " + request.getMethod()+" 方法");
        }
        String username = obtainUsername(request);
        String password = obtainPassword(request);
        String encryptedAesKey = obtainWebAesKey(request);
        String clientRsaId = obtainClientRsaId(request);


        checkNecessaryParameters(username, password,encryptedAesKey,clientRsaId);


        Map<String, String> map ;

        try {
           map = decryptUserAndPassword(username, password, encryptedAesKey, clientRsaId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        username = map.get("username");
        password = map.get("password");

        checkNecessaryParameters(username, password,encryptedAesKey,clientRsaId);
        username = username.trim();

        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);
        // Allow subclasses to set the "details" property
        setDetails(request, authRequest);
        return this.getAuthenticationManager().authenticate(authRequest);
    }



    /***
     *  对用户名和密码进行解密
     * */
    private  Map<String, String> decryptUserAndPassword(String username, String password,String webAesKey,String clientRsaId) throws Exception {

        EncryptedRecords clientRsaRecords = encrytedRecordHelper.getEncryptedRecords(Long.valueOf(clientRsaId));
        byte[] bytes = RsaUtil.decryptByPrivateKey(Base64.getDecoder().decode(webAesKey), clientRsaRecords.getClientRsaPrivatekey());
        String  decryptWebAesKey = new String(bytes);

        Map<String, String> map = new HashMap<>();
        map.put("username", AESUtils.decrypt(username, decryptWebAesKey));
        map.put("password", AESUtils.decrypt(password, decryptWebAesKey));
        return map;

    }

    /**
     * 检查必要的参数
     */
    private void checkNecessaryParameters(String username, String password, String encryptedAesKey, String clientRsaId) {
        if (StringUtils.isEmpty(encryptedAesKey) || StringUtils.isEmpty(clientRsaId)) {
            //密码强制加密，如果没有填写webAesKey，就有问题
            throw new AuthenticationServiceException("登陆出现错误：login_001,请联系管理员");
        }
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            throw new AuthenticationServiceException("用户名或密码不能为空");
        }
    }


    @Nullable
    protected String obtainPassword(HttpServletRequest request) {
        return request.getParameter(this.passwordParameter);
    }


    @Nullable
    protected String obtainUsername(HttpServletRequest request) {
        return request.getParameter(this.usernameParameter);
    }

    @Nullable
    protected String obtainWebAesKey(HttpServletRequest request) {
        return request.getParameter(this.webAesKeyParameter);
    }

    private String obtainClientRsaId(HttpServletRequest request) {
        return request.getParameter(this.clientRsaIdParameter);
    }


    protected void setDetails(HttpServletRequest request, UsernamePasswordAuthenticationToken authRequest) {
        authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
    }


    public void setUsernameParameter(String usernameParameter) {
        Assert.hasText(usernameParameter, "Username parameter must not be empty or null");
        this.usernameParameter = usernameParameter;
    }


    public void setPasswordParameter(String passwordParameter) {
        Assert.hasText(passwordParameter, "Password parameter must not be empty or null");
        this.passwordParameter = passwordParameter;
    }


    public void setPostOnly(boolean postOnly) {
        this.postOnly = postOnly;
    }

    public final String getUsernameParameter() {
        return this.usernameParameter;
    }

    public final String getPasswordParameter() {
        return this.passwordParameter;
    }


}
