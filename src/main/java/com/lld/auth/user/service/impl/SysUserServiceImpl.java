package com.lld.auth.user.service.impl;

import com.lld.auth.redis.lua.LuaScriptManager;
import com.lld.auth.security.entity.MyUsernamePasswordAuthenticationToken;
import com.lld.auth.user.entity.DTO.SysUserDto;
import com.lld.auth.user.entity.EncryptedRecords;
import com.lld.auth.user.entity.OnlineStatistics;
import com.lld.auth.user.entity.SysUser;
import com.lld.auth.user.entity.VO.SysUserVo;
import com.lld.auth.user.entity.WebMenu;
import com.lld.auth.user.mapper.EncryptedRecordsMapper;
import com.lld.auth.user.mapper.SysUserMapper;
import com.lld.auth.user.mapstruct.MSUserMapper;
import com.lld.auth.user.repository.SysUserRepository;
import com.lld.auth.user.service.LuaOnlineUserService;
import com.lld.auth.user.service.SysMenuService;
import com.lld.auth.user.service.SysRoleService;
import com.lld.auth.user.service.SysUserService;
import com.lld.auth.utils.AuthPublicConstantKeys;
import com.lld.auth.utils.SecurityUserUtils;
import com.lld.saltedfishutils.entity.WebComponentVO.SelectOptionVO;
import com.lld.saltedfishutils.redis.RedisUtil;
import com.lld.saltedfishutils.web.result.ReturnResult;
import com.lld.saltedfishutils.crypto.RSAUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author saltedFish
 * @description 针对表【sys_user】的数据库操作Service实现
 * @createDate 2024-03-20 15:45:56
 */
@Service
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;

    private final EncryptedRecordsMapper encryptedRecordsMapper;

    private final PasswordEncoder passwordEncoder;

    private final SysMenuService sysMenuService;

    private MSUserMapper msUserMapper = MSUserMapper.INSTANCE;

    private SysRoleService sysRoleService;

    private RedisUtil redisUtil;
    // lua在线用户服务
    private LuaOnlineUserService luaOnlineUserService;
    // lua脚本管理器
    private LuaScriptManager luaScriptManager;
    // stringRedisTemplate
    private StringRedisTemplate stringRedisTemplate;
    //SysUserRepository
    private SysUserRepository sysUserRepository;

    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    public SysUserServiceImpl(SysUserMapper sysUserMapper, EncryptedRecordsMapper encryptedRecordsMapper,
                              PasswordEncoder passwordEncoder, SysMenuService sysMenuService, SysRoleService sysRoleService,
                              RedisUtil redisUtil, LuaOnlineUserService luaOnlineUserService, LuaScriptManager luaScriptManager,
                              StringRedisTemplate stringRedisTemplate, SysUserRepository sysUserRepository) {
        this.sysUserMapper = sysUserMapper;
        this.encryptedRecordsMapper = encryptedRecordsMapper;
        this.passwordEncoder = passwordEncoder;
        this.sysMenuService = sysMenuService;
        this.sysRoleService = sysRoleService;
        this.redisUtil = redisUtil;
        this.luaOnlineUserService = luaOnlineUserService;
        this.luaScriptManager = luaScriptManager;
        this.stringRedisTemplate = stringRedisTemplate;
        this.sysUserRepository = sysUserRepository;
    }


    @Override
    public SysUser getUserByUserName(String username) {
        SysUser byUserName = sysUserMapper.getByUserName(username);
        return byUserName;
    }

    @Override
    public ReturnResult registerUser(SysUser sysUser) {
        checkRegisterUser(sysUser);
        //使用注册的密码器加密
        String encode = passwordEncoder.encode(sysUser.getPassword());
        sysUser.setPassWord(encode);
        //设置注册用户默认值
        sysUser.setStatus("1");
        Date date = new Date();
        sysUser.setCreateTime(date);
        sysUser.setUpdateTime(date);
        sysUser.setRoles("3");
        sysUserRepository.insertSingle(sysUser);
        return ReturnResult.OK();
    }

    @Override
    public void updateLoginDate(Long id, Date loginDate) {
        sysUserRepository.updateLoginDate(id, loginDate);

    }

    /**
     * 获取加密公钥
     * **/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnResult getClientRsaPublicKey() throws Exception {
        //先看数据库是否有最新的公私钥
        EncryptedRecords latestRecored = encryptedRecordsMapper.selectLatestEncryptedRecord();
        if(latestRecored != null){
            latestRecored.setClientRsaPrivatekey(null);
            return ReturnResult.OK(latestRecored);
        }
        //生成客户端RSA公钥私钥
        Map<String, String> rsaMap = RSAUtil.genKeyPair();
        //保存到数据库
        EncryptedRecords encryptedRecords = new EncryptedRecords();
        encryptedRecords.setClientRsaPublickey(rsaMap.get("publicKey"));
        encryptedRecords.setClientRsaPrivatekey(rsaMap.get("privateKey"));
        Date date = new Date();
        encryptedRecords.setCreateTime(date);
        encryptedRecords.setUpdateTime(date);
        encryptedRecords.setType(0);
        encryptedRecords.setStatus(1);
        encryptedRecordsMapper.addEncryptedRecord(encryptedRecords);
        //不返回客户端私钥
        encryptedRecords.setClientRsaPrivatekey(null);
        return ReturnResult.OK(encryptedRecords);
    }

    @Override
    public ReturnResult getMenus() {

        List<WebMenu> userMenus = sysMenuService.getUserMenus();
        return ReturnResult.OK(userMenus);
    }

    @Override
    public ReturnResult getUserList() {
        List<SysUser> sysUsers = sysUserRepository.selectList(null);
        List<SysUserVo> sysUserVos = new ArrayList<>();
        Map<Long, String> rolesMap = sysRoleService.getRoleMap();
        for (SysUser sysUser : sysUsers) {
            SysUserVo sysUserVo = msUserMapper.SysUserToSysUserVo(sysUser);
            //单独设置userName字段
            sysUserVo.setUserName(sysUser.getUsername());
            //设置角色名称
            setSysUserVoSomeFields(rolesMap,sysUserVo);

            sysUserVos.add(sysUserVo);
        }

        return ReturnResult.OK(sysUserVos);
    }
    /**
     * 对sysUserVo某些字段进行补充
     * **/
    public void setSysUserVoSomeFields(Map<Long, String> rolesMap ,SysUserVo sysUserVo){
        String[] roleSplit = sysUserVo.getRoles().split(",");
        StringBuilder roleBuilder = new StringBuilder();
        for (String role : roleSplit) {
            String roleName = rolesMap.get(Long.valueOf(role));
            roleBuilder.append(roleName).append(",");
        }
        sysUserVo.setRolesStr(roleBuilder.substring(0,roleBuilder.length()-1).toString());
        //设置状态名称
        sysUserVo.setStatusStr(sysUserVo.getStatus().equals("1")?"正常":"禁用");
    }

    @Override
    public ReturnResult getUserRolesTree() {
        List<SelectOptionVO> selectOptionVOS = new ArrayList<>();
//        selectOptionVOS.add(new SelectOptionVO("超级管理员","1","1",null));
        selectOptionVOS.add(new SelectOptionVO("管理员","2","2",null));
        selectOptionVOS.add(new SelectOptionVO("普通用户","3","3",null));
        selectOptionVOS.add(new SelectOptionVO("剑三er","4","4",null));
        selectOptionVOS.add(new SelectOptionVO("谋定天下er","5","5",null));

        return ReturnResult.OK(selectOptionVOS);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnResult addUser(SysUserDto sysUserDto) {
        MyUsernamePasswordAuthenticationToken authentication = (MyUsernamePasswordAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        Long userId = authentication.getUserId();
        SysUser byUserName = sysUserMapper.getByUserName(sysUserDto.getUserName());
        if(byUserName != null){
            throw new RuntimeException("用户名已存在");
        }
        SysUser sysUser = msUserMapper.SysUserDtoToSysUser(sysUserDto);
        String encodePassWord = passwordEncoder.encode(sysUser.getPassword());
        sysUser.setPassWord(encodePassWord);
        sysUser.setCreater(userId);
        sysUser.setUpdater(userId);
        Date now = new Date();
        sysUser.setCreateTime(now);
        sysUser.setUpdateTime(now);
        sysUserMapper.insertSingle(sysUser);
        return ReturnResult.OK();
    }

    @Override
    public ReturnResult updateUser(SysUserDto sysUserDto) {
        MyUsernamePasswordAuthenticationToken authentication = (MyUsernamePasswordAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        Long userId = authentication.getUserId();
        SysUser sysUser = msUserMapper.SysUserDtoToSysUser(sysUserDto);
        Boolean superAdmin = Arrays.asList(sysUser.getRoles().split(",")).contains("1");
        if(superAdmin){
            throw new RuntimeException("用户暂时不能赋予超级管理员权限");
        }
        sysUser.setUpdater(userId);
        Date now = new Date();
        sysUser.setUpdateTime(now);
        int i = sysUserMapper.updateUserById(sysUser);
        if(i == 0){
            return ReturnResult.error("未找到用户");
        }
        return ReturnResult.OK();
    }

    @Override
    public ReturnResult getUserById(Long id) {
        SysUser sysUser = sysUserRepository.selectById(id);
        if(sysUser != null){
            SysUserVo sysUserVo = msUserMapper.SysUserToSysUserVo(sysUser);
            sysUserVo.setUserName(sysUser.getUsername());

            return ReturnResult.OK(sysUserVo);
        }
        return ReturnResult.error("未找到用户");
    }

    @Override
    public ReturnResult deleteById(SysUserDto sysUserDto) {

        int delete = sysUserRepository.deleteById(sysUserDto.getId());
        if (delete == 0){
            return ReturnResult.error("未找到用户");
        }
        return ReturnResult.OK();
    }

    /**
     * 根据用户id获取用户角色
     * **/
    @Override
    public List<String> getRolesByUserId(Long userId) {
        SysUser sysUser = sysUserMapper.selectById(userId);
        List<Long> roleIds = Arrays.stream(sysUser.getRoles().split(",")).map(item -> Long.parseLong(item)).collect(Collectors.toList());
        List<String> rolesById = sysRoleService.getRolesById(roleIds);
        return rolesById;
    }

    /**
     * 根据用户id获取用户详情
     * */
    @Override
    public ReturnResult getUserDetailById(Long  userId) {
        SysUser sysUser = sysUserMapper.selectById(userId);
        SysUserVo sysUserVo = msUserMapper.SysUserToSysUserVo(sysUser);
        sysUserVo.setUserName(sysUser.getUsername());
        Map<Long, String> rolesMap = sysRoleService.getRoleMap();
        setSysUserVoSomeFields(rolesMap,sysUserVo);
        return ReturnResult.OK(sysUserVo);
    }

    @Override
    public ReturnResult userHeartBeat(Long id, Integer status) {
        Long userId = SecurityUserUtils.getCurrentUserId();
        long currentTimeMillis = System.currentTimeMillis();
        String todayOnlineBitmapKey = AuthPublicConstantKeys.TODAY_ONLINE_BITMAP_PREFIX+LocalDate.now().format(DATE_FORMATTER);

        //获取三个key
        List<String> keys = Arrays.asList(
                AuthPublicConstantKeys.ONLINE_BITMAP_KEY,   //onlineBitmapKey 在线用户bitmap
                AuthPublicConstantKeys.ACTIVITY_ZSET_KEY,  //activityZsetKey 活跃用户zset
                todayOnlineBitmapKey
        );

        //登录脚本参数
        String[] argv = {

                String.valueOf(userId), //用户id
                String.valueOf(currentTimeMillis),

        };

        RedisScript<List> userHeartbeatScript = luaScriptManager.getUserHeartbeatScript();

        List<Object> result = (List<Object>) stringRedisTemplate.execute(
                userHeartbeatScript,
                keys, argv
        );

        return ReturnResult.OK(result);
    }

    @Override
    public ReturnResult getOnlineStatistics() {
        OnlineStatistics onlineStatistics = luaOnlineUserService.getOnlineStatistics();

        return ReturnResult.OK(onlineStatistics);
    }

    private void checkRegisterUser(SysUser sysUser) {
        if (sysUser.getUsername() == null || sysUser.getUsername().length() == 0) {
            throw new RuntimeException("用户名不能为空");
        }
        if (sysUser.getPassword() == null || sysUser.getPassword().length() == 0) {
            throw new RuntimeException("密码不能为空");
        }
        if (sysUser.getEmail() == null || sysUser.getEmail().length() == 0) {
            throw new RuntimeException("邮箱不能为空");
        }else {
             final String EMAIL_PATTERN = "^\\S+@\\S+\\.\\S+$";
            Pattern pattern = Pattern.compile(EMAIL_PATTERN);
            Matcher matcher = pattern.matcher(sysUser.getEmail());

            // 使用matcher的matches方法进行完整字符串匹配
            if(!matcher.matches()){
                throw new RuntimeException("邮箱格式不正确");
            }

        }
        String username = sysUser.getUsername();
        int countUserName = sysUserRepository.getCountUserName(username);
        if (countUserName > 0) {
            throw new RuntimeException("用户名已存在");
        }
    }
}




