package com.lld.auth.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lld.auth.user.entity.EncryptedRecords;
import com.lld.auth.user.entity.SysUser;
import com.lld.auth.user.mapper.EncryptedRecordsMapper;
import com.lld.auth.user.service.SysUserService;
import com.lld.auth.user.mapper.SysUserMapper;
import com.lld.saltedfishutils.utils.ReturnResult;
import com.lld.saltedfishutils.utils.RsaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
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

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private EncryptedRecordsMapper encryptedRecordsMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

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




