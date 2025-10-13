package com.lld.auth.user.mapper;

import com.lld.auth.user.entity.RoleMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author saltedFish
* @description 针对表【role_menu】的数据库操作Mapper
* @createDate 2025-07-02 22:06:29
* @Entity com.lld.auth.user.entity.RoleMenu
*/
@Mapper
public interface RoleMenuMapper extends BaseMapper<RoleMenu> {

    int deleteByRoleId(@Param("roleId") Long roleId);

    int insertRoleMenus(List<RoleMenu> roleMenus);

    List<RoleMenu>  selectByRoleId(@Param("roleId") Long roleId);
}




