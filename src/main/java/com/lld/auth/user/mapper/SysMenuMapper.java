package com.lld.auth.user.mapper;

import com.lld.auth.user.entity.SysMenu;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
* @author saltedFish
* @description 针对表【sys_menu】的数据库操作Mapper
* @createDate 2024-09-17 02:49:00
* @Entity com.lld.auth.user.entity.SysMenu
*/
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {


    int updateMenuById(@Param("sysMenu") SysMenu sysMenu);
}




