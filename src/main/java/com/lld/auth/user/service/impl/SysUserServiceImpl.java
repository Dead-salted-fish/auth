package com.lld.auth.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lld.auth.security.entity.MyUsernamePasswordAuthenticationToken;
import com.lld.auth.user.entity.DTO.SysUserDto;
import com.lld.auth.user.entity.EncryptedRecords;
import com.lld.auth.user.entity.SysUser;
import com.lld.auth.user.entity.VO.SysUserVo;
import com.lld.auth.user.entity.WebMenu;
import com.lld.auth.user.mapper.EncryptedRecordsMapper;
import com.lld.auth.user.mapstruct.MSUserMapper;
import com.lld.auth.user.service.SysMenuService;
import com.lld.auth.user.service.SysUserService;
import com.lld.auth.user.mapper.SysUserMapper;
import com.lld.saltedfishutils.entity.WebComponentVO.SelectOptionVO;
import com.lld.saltedfishutils.utils.ReturnResult;
import com.lld.saltedfishutils.utils.RsaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author saltedFish
 * @description 针对表【sys_user】的数据库操作Service实现
 * @createDate 2024-03-20 15:45:56
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
        implements SysUserService {

    private final SysUserMapper sysUserMapper;

    private final EncryptedRecordsMapper encryptedRecordsMapper;

    private final PasswordEncoder passwordEncoder;

    private final SysMenuService sysMenuService;

    private MSUserMapper msUserMapper = MSUserMapper.INSTANCE;

    public SysUserServiceImpl(SysUserMapper sysUserMapper, EncryptedRecordsMapper encryptedRecordsMapper, PasswordEncoder passwordEncoder, SysMenuService sysMenuService) {
        this.sysUserMapper = sysUserMapper;
        this.encryptedRecordsMapper = encryptedRecordsMapper;
        this.passwordEncoder = passwordEncoder;
        this.sysMenuService = sysMenuService;
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

        baseMapper.insertSingle(sysUser);
        return ReturnResult.OK();
    }

    @Override
    public void updateLoginDate(Long id, Date loginDate) {
        baseMapper.updateLoginDate(id, loginDate);
    }

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
        Map<String, String> rsaMap = RsaUtil.genKeyPair();
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
        List<SysUser> sysUsers = baseMapper.selectList(null);
        List<SysUserVo> sysUserVos = new ArrayList<>();
        for (SysUser sysUser : sysUsers) {
            SysUserVo sysUserVo = msUserMapper.SysUserToSysUserVo(sysUser);
            sysUserVo.setUserName(sysUser.getUsername());
            String[] roleSplit = sysUser.getRoles().split(",");
            StringBuilder roleBuilder = new StringBuilder();
            for (String role : roleSplit) {
                switch (role){
                    case "1":
                        roleBuilder.append("超级管理员").append(",");
                        break;
                    case "2":
                        roleBuilder.append("管理员").append(",");
                        break;
                    case "3":
                        roleBuilder.append("普通用户").append(",");
                        break;
                    case "4":
                        roleBuilder.append("剑三er").append(",");
                        break;
                    case "5":
                        roleBuilder.append("谋定天下er").append(",");
                }
            }
            sysUserVo.setRolesStr(roleBuilder.substring(0,roleBuilder.length()-1).toString());

            sysUserVos.add(sysUserVo);
        }

        return ReturnResult.OK(sysUserVos);
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
        SysUser sysUser = baseMapper.selectById(id);
        if(sysUser != null){
            SysUserVo sysUserVo = msUserMapper.SysUserToSysUserVo(sysUser);
            sysUserVo.setUserName(sysUser.getUsername());

            return ReturnResult.OK(sysUserVo);
        }
        return ReturnResult.error("未找到用户");
    }

    @Override
    public ReturnResult deleteById(SysUserDto sysUserDto) {
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",sysUserDto.getId());
        int delete = baseMapper.delete(queryWrapper);
        if (delete == 0){
            return ReturnResult.error("未找到用户");
        }
        return ReturnResult.OK();
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
        int countUserName = baseMapper.getCountUserName(username);
        if (countUserName > 0) {
            throw new RuntimeException("用户名已存在");
        }
    }
}




