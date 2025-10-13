package com.lld.auth.user.mapper;

import com.lld.auth.user.entity.SysRole;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* @author saltedFish
* @description 针对表【sys_role】的数据库操作Mapper
* @createDate 2025-05-28 14:04:27
* @Entity com.lld.auth.user.entity.SysRole
*/
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    List<SysRole> selectByIds(List<Long> roleIds);
}




