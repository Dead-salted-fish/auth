package com.lld.auth.user.service.impl;

import com.lld.auth.security.entity.MyUsernamePasswordAuthenticationToken;
import com.lld.auth.user.entity.*;
import com.lld.auth.user.entity.DTO.SysUserDto;
import com.lld.auth.user.entity.VO.ClientRsaPublicKeyVO;
import com.lld.auth.user.entity.VO.SysUserVo;
import com.lld.auth.user.mapper.EncryptedRecordsMapper;
import com.lld.auth.user.mapper.SysUserMapper;
import com.lld.auth.user.mapstruct.MSUserMapper;
import com.lld.auth.user.repository.SysUserRepository;
import com.lld.auth.user.service.*;
import com.lld.auth.utils.AuthPublicConstantKeys;
import com.lld.auth.utils.SecurityUserUtils;
import com.lld.saltedfishutils.crypto.jwt.TokenUtil;
import com.lld.saltedfishutils.crypto.kek.KekEnvelope;
import com.lld.saltedfishutils.entity.WebComponentVO.SelectOptionVO;
import com.lld.saltedfishutils.redis.RedisUtil;
import com.lld.saltedfishutils.web.result.ReturnResult;
import com.lld.saltedfishutils.crypto.RSAUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

    // stringRedisTemplate
    private StringRedisTemplate stringRedisTemplate;
    //SysUserRepository
    // SysUserRepository
    private SysUserRepository sysUserRepository;

    private IUserRoleService userRoleService;
    private TokenUtil tokenUtil;

    private final KekEnvelope kekEnvelope;

    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    private final RedissonClient redissonClient;

    /** 全局固定的锁名：所有 auth 实例共享同一把锁；此时还没有 keyId，不能用 id 拼 key */
    private static final String ACTIVE_RSA_LOCK = "lock:auth:active-rsa-key";

    /** 等待锁的时间，要覆盖一次 2048 位 RSA 生成 + insert 的耗时 */
    private static final long LOCK_WAIT_SECONDS = 10;


    public SysUserServiceImpl(SysUserMapper sysUserMapper, EncryptedRecordsMapper encryptedRecordsMapper,
                              PasswordEncoder passwordEncoder, SysMenuService sysMenuService, SysRoleService sysRoleService,
                              RedisUtil redisUtil, LuaOnlineUserService luaOnlineUserService,
                              StringRedisTemplate stringRedisTemplate, SysUserRepository sysUserRepository,
                              IUserRoleService userRoleService, TokenUtil tokenUtil,
                              KekEnvelope kekEnvelope, RedissonClient redissonClient) {
        this.sysUserMapper = sysUserMapper;
        this.encryptedRecordsMapper = encryptedRecordsMapper;
        this.passwordEncoder = passwordEncoder;
        this.sysMenuService = sysMenuService;
        this.sysRoleService = sysRoleService;
        this.redisUtil = redisUtil;
        this.luaOnlineUserService = luaOnlineUserService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.sysUserRepository = sysUserRepository;
        this.userRoleService = userRoleService;
        this.tokenUtil = tokenUtil;
        this.kekEnvelope = kekEnvelope;
        this.redissonClient = redissonClient;
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
        // 默认 1 为正常状态
        sysUser.setStatus("1");
        Date date = new Date();
        sysUser.setCreateTime(date);
        sysUser.setUpdateTime(date);
        // 设置默认角色为3，普通用户
        userRoleService.updateUserRoles(sysUser.getId(), "3", new Date());
        sysUserRepository.insertSingle(sysUser);
        return ReturnResult.OK();
    }

    @Override
    public void updateLoginDate(Long id, Date loginDate) {
        sysUserRepository.updateLoginDate(id, loginDate);

    }

    /**
     * 返回当前 active 的公钥与 keyId。
     * 公钥列明文入库、明文返回；私钥列密文入库，此接口完全不触碰。
     * type=0,status=1 全局唯一（DDL 保证）；status=0 为历史密钥，永久保留供历史数据解密。
     */
    @Override
    public ReturnResult getClientRsaPublicKey() throws Exception {
        EncryptedRecords active = encryptedRecordsMapper.selectLatestEncryptedRecord();
        if (active == null) {
            active = createActiveKeyPair();
        }
        ClientRsaPublicKeyVO vo = new ClientRsaPublicKeyVO();
        vo.setId(active.getId());
        vo.setClientRsaPublickey(active.getClientRsaPublickey());
        return ReturnResult.OK(vo);
    }

    /** 首次生成：并发时靠唯一索引兜底，DuplicateKeyException 后重查已有的 active */
    private EncryptedRecords createActiveKeyPair() throws Exception {
        RLock lock = redissonClient.getLock(ACTIVE_RSA_LOCK);
        boolean locked = false;
        try {
            // leaseTime = -1 → 启用 Redisson watchdog 自动续期（与 MDTX 用法一致），
            // 避免 RSA 生成偶发变慢导致锁提前过期
            locked = lock.tryLock(LOCK_WAIT_SECONDS, -1, TimeUnit.SECONDS);

            if (!locked) {
                // 等待超时：持锁方可能刚好完成，再查一次；仍为空则让前端重试
                EncryptedRecords latest = encryptedRecordsMapper.selectLatestEncryptedRecord();
                if (latest == null) {
                    throw new IllegalStateException("active RSA 密钥正在生成中，请稍后重试");
                }
                return latest;
            }

            // 关键：锁内双检，防止前一个加锁请求已经生成过最新的了
            EncryptedRecords latest = encryptedRecordsMapper.selectLatestEncryptedRecord();
            if (latest != null) {
                return latest;
            }

            Map<String, String> rsaMap = RSAUtil.genKeyPair();
            Date date = new Date();

            EncryptedRecords record = new EncryptedRecords();
            record.setClientRsaPublickey(rsaMap.get("publicKey"));                    // 不 seal
            record.setClientRsaPrivatekey(kekEnvelope.seal(rsaMap.get("privateKey"))); // 只 seal 私钥
            record.setCreateTime(date);
            record.setUpdateTime(date);
            record.setType(0);
            record.setStatus(1);

            try {
                encryptedRecordsMapper.addEncryptedRecord(record);
            } catch (DuplicateKeyException e) {
                // 最终兜底：锁过期 / Redis 故障 / 有人绕过此接口直接插入时，
                // 唯一索引仍保证只有一把 active。此时已无外层事务，重查可见。
                EncryptedRecords existing = encryptedRecordsMapper.selectLatestEncryptedRecord();
                if (existing == null) throw e;
                return existing;
            }
            return record;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待 active RSA 密钥锁被中断", e);
        } finally {
            // 必须判断持有者，避免锁超时后误删别人的锁
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public ReturnResult getMenus() {

        List<WebMenu> userMenus = sysMenuService.getUserMenus();
        return ReturnResult.OK(userMenus);
    }

    @Override
    public ReturnResult getUserList() {
        // 获取所有用户
        List<SysUser> sysUsers = sysUserRepository.selectList(null);
        //获取角色名称
        Map<Long, String> rolesMap = sysRoleService.getRoleMap();
        // 获取用户的角色
        List<Long> userIds = sysUsers.stream().map(SysUser::getId).collect(Collectors.toList());
        Map<Long, List<Long>> userRolesMap = userRoleService.getUserRolesByUserIds(userIds)
                .stream()
                .collect(Collectors.groupingBy(
                        UserRole::getUserId,
                        Collectors.mapping(UserRole::getRoleId, Collectors.toList())
                ));
        List<SysUserVo> sysUserVos = new ArrayList<>();
        for (SysUser sysUser : sysUsers) {
            SysUserVo sysUserVo = msUserMapper.SysUserToSysUserVo(sysUser);
            //单独设置userName字段
            sysUserVo.setUserName(sysUser.getUsername());
            //设置角色名称
            setSysUserVoSomeFields(rolesMap, sysUserVo, userRolesMap.get(sysUser.getId()));

            sysUserVos.add(sysUserVo);
        }

        return ReturnResult.OK(sysUserVos);
    }

    /**
     * 对sysUserVo某些字段进行补充
     *
     **/
    public void setSysUserVoSomeFields(Map<Long, String> rolesMap, SysUserVo sysUserVo, List<Long> roleIds) {

        StringBuilder roleBuilder = new StringBuilder();
        if (!CollectionUtils.isEmpty(roleIds)) {
            for (Long roleId : roleIds) {
                roleBuilder.append(rolesMap.get(roleId)).append(",");
            }
            sysUserVo.setRolesStr(roleBuilder.substring(0, roleBuilder.length() - 1).toString());
        }
        //设置状态名称
        sysUserVo.setStatusStr(sysUserVo.getStatus().equals("1") ? "正常" : "禁用");
    }

    @Override
    public ReturnResult getUserRolesTree() {
        List<SelectOptionVO> selectOptionVOS = new ArrayList<>();
//        selectOptionVOS.add(new SelectOptionVO("超级管理员","1","1",null));
        selectOptionVOS.add(new SelectOptionVO("管理员", "2", "2", null));
        selectOptionVOS.add(new SelectOptionVO("普通用户", "3", "3", null));
        selectOptionVOS.add(new SelectOptionVO("剑三er", "4", "4", null));
        selectOptionVOS.add(new SelectOptionVO("谋定天下er", "5", "5", null));

        return ReturnResult.OK(selectOptionVOS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnResult addUser(SysUserDto sysUserDto) {
        MyUsernamePasswordAuthenticationToken authentication = (MyUsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        Long userId = authentication.getUserId();
        SysUser byUserName = sysUserMapper.getByUserName(sysUserDto.getUserName());
        if (byUserName != null) {
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
        // 检查用户是否是超级管理员
        Boolean superAdmin = Arrays.asList(sysUserDto.getRoles().split(",")).contains("1");
        if (superAdmin) {
            throw new RuntimeException("用户暂时不能赋予超级管理员权限");
        }
        // 新增用户 更新用户角色
        sysUserMapper.insertSingle(sysUser);
        userRoleService.updateUserRoles(sysUser.getId(), sysUserDto.getRoles(), now);
        return ReturnResult.OK();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnResult updateUser(SysUserDto sysUserDto) {
        MyUsernamePasswordAuthenticationToken authentication = (MyUsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        Long userId = authentication.getUserId();
        SysUser sysUser = msUserMapper.SysUserDtoToSysUser(sysUserDto);
        Boolean superAdmin = Arrays.asList(sysUserDto.getRoles().split(",")).contains("1");
        if (superAdmin) {
            throw new RuntimeException("用户暂时不能赋予超级管理员权限");
        }
        sysUser.setUpdater(userId);
        Date now = new Date();
        sysUser.setUpdateTime(now);
        // 更新用户
        int i = sysUserMapper.updateUserById(sysUser);
        if (i == 0) {
            return ReturnResult.error("未找到用户");
        }
        // 更新用户角色
        userRoleService.updateUserRoles(sysUser.getId(), sysUserDto.getRoles(), now);
        return ReturnResult.OK();
    }

    @Override
    public ReturnResult getUserById(Long id) {
        SysUser sysUser = sysUserRepository.selectById(id);
        List<UserRole> userRolesByUserIds = userRoleService.getUserRolesByUserIds(List.of(id));
        if (sysUser != null) {
            SysUserVo sysUserVo = msUserMapper.SysUserToSysUserVo(sysUser);
            sysUserVo.setUserName(sysUser.getUsername());
            sysUserVo.setRoles(userRolesByUserIds.stream()
                    .map(UserRole::getRoleId)
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));

            return ReturnResult.OK(sysUserVo);
        }
        return ReturnResult.error("未找到用户");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnResult deleteById(SysUserDto sysUserDto) {
        Long userId = sysUserDto.getId();
        int delete = sysUserRepository.deleteById(userId);
        if (delete == 0) {
            return ReturnResult.error("未找到用户");
        }
        userRoleService.deleteUserRolesByUserId(userId);
        return ReturnResult.OK();
    }

    /**
     * 根据用户id获取用户角色
     *
     **/
    @Override
    public List<String> getRolesByUserId(Long userId) {
        List<UserRole> userRolesByUserIds = userRoleService.getUserRolesByUserIds(List.of(userId));
        //获取角色名称
        List<SysRole> roles = sysRoleService.getAllRoles();

        // 获取用户的角色名
        return userRolesByUserIds.stream().
                map(userRole -> {
                            return roles.stream()
                                    .filter(role -> role.getId().equals(userRole.getRoleId()))
                                    .map(SysRole::getRole)
                                    .findFirst()
                                    .orElse(null);
                        }

                ).collect(Collectors.toList());

    }

    /**
     * 根据用户id获取用户详情
     *
     */
    @Override
    public ReturnResult getUserDetailById(Long userId) {
        SysUser sysUser = sysUserMapper.selectById(userId);
        SysUserVo sysUserVo = msUserMapper.SysUserToSysUserVo(sysUser);
        sysUserVo.setUserName(sysUser.getUsername());
        Map<Long, String> rolesMap = sysRoleService.getRoleMap();
        // 获取用户的角色
        Map<Long, List<Long>> userRolesMap = userRoleService.getUserRolesByUserIds(List.of(userId))
                .stream()
                .collect(Collectors.groupingBy(
                        UserRole::getUserId,
                        Collectors.mapping(UserRole::getRoleId, Collectors.toList())
                ));

        setSysUserVoSomeFields(rolesMap, sysUserVo, userRolesMap.get(userId));
        return ReturnResult.OK(sysUserVo);
    }


    @Override
    public ReturnResult getOnlineStatistics() {
        OnlineStatistics onlineStatistics = luaOnlineUserService.getOnlineStatistics();

        return ReturnResult.OK(onlineStatistics);
    }

    /**
     * 返回双token 刷新的 accessToken
     * */
    @Override
    public ReturnResult getAccesstoken() {
        Map<String, Object> accessTokenClaims = generateToken("accessToken");
        return ReturnResult.OK(accessTokenClaims.get("token"));
    }

    @Override
    public String getUserAuthorities(String userId) {
        // 获取用户角色
        List<UserRole> userRoles = userRoleService.getUserRolesByUserIds(Collections.singletonList(Long.valueOf(userId)));
        //  获取所有角色
        Map<Long, String> roleMap = sysRoleService.getAllRoles().stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRole));

        List<String> authoritiesStrList = new ArrayList<>();
        for (UserRole userRole : userRoles) {
            Long roleId = userRole.getRoleId();
            authoritiesStrList.add("ROLE_" + roleMap.get(roleId));
        }
        return String.join(",", authoritiesStrList);

    }

    /**
     *  返回双token 刷新的 refreshToken
     * */
    @Override
    public ReturnResult getRefreshToken() {

        Map<String, Object> accessTokenClaims = generateToken("refreshToken");
        return ReturnResult.OK(accessTokenClaims.get("token"));
    }

    /** 生成 token  */
    private Map<String, Object> generateToken(String versionType) {
        Long currentUserId = SecurityUserUtils.getCurrentUserId();
        SysUser sysUser = sysUserMapper.selectById(currentUserId);
        if (sysUser == null) {
            throw new RuntimeException("未找到用户");
        }

        Map<String, Object> customClaims = new HashMap<>();
        customClaims.put("version", sysUser.getVersion());
        Map<String, Object> accessTokenClaims = null;
        if (versionType.equals("accessToken")) {
            accessTokenClaims = tokenUtil.generateAccessToken(sysUser.getId(), sysUser.getUsername(), customClaims);
        } else if (versionType.equals("refreshToken")) {
            accessTokenClaims = tokenUtil.generateRefreshToken(sysUser.getId(), sysUser.getUsername(), customClaims);
        }
        return accessTokenClaims;
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
        } else {
            final String EMAIL_PATTERN = "^\\S+@\\S+\\.\\S+$";
            Pattern pattern = Pattern.compile(EMAIL_PATTERN);
            Matcher matcher = pattern.matcher(sysUser.getEmail());

            // 使用matcher的matches方法进行完整字符串匹配
            if (!matcher.matches()) {
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




