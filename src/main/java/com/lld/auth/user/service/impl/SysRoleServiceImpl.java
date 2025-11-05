package com.lld.auth.user.service.impl;

import com.lld.auth.user.entity.DTO.SysRoleDto;
import com.lld.auth.user.entity.SysRole;
import com.lld.auth.user.entity.VO.SysRoleVo;
import com.lld.auth.user.mapstruct.MSSysRoleMapper;
import com.lld.auth.user.repository.SysRoleRepository;
import com.lld.auth.user.service.RoleMenuService;
import com.lld.auth.user.service.SysRoleService;
import com.lld.auth.utils.SecurityUserUtils;
import com.lld.saltedfishutils.entity.WebComponentVO.SelectOptionVO;
import com.lld.saltedfishutils.web.result.ReturnResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author saltedFish
 * @description 针对表【sys_role】的数据库操作Service实现
 * @createDate 2025-05-28 14:04:27
 */
@Service
public class SysRoleServiceImpl implements SysRoleService {
    private MSSysRoleMapper msSysRoleMapper = MSSysRoleMapper.INSTANCE;

    private final SysRoleRepository sysRoleRepository;
    private final RoleMenuService roleMenuService;

    public SysRoleServiceImpl(RoleMenuService roleMenuService, SysRoleRepository sysRoleRepository) {
        this.roleMenuService = roleMenuService;
        this.sysRoleRepository = sysRoleRepository;
    }

    @Override
    public ReturnResult getRolesList() {

        List<SysRole> sysRoles = sysRoleRepository.selectList();
        List<SysRoleVo> sysRoleVos = msSysRoleMapper.sysRolesListToSysRoleVoList(sysRoles);
        for (SysRoleVo sysRoleVo : sysRoleVos) {
            supplementSysRoleVoSomeFields(sysRoleVo);
        }
        return ReturnResult.OK(sysRoleVos);
    }

    @Override
    public ReturnResult addRole(SysRoleDto sysRoleDto) {
        Long userId = SecurityUserUtils.getCurrentUserId();
        SysRole sysRole = msSysRoleMapper.sysRoleDtoToSysRole(sysRoleDto);

        sysRole.setCreater(userId);
        sysRole.setUpdater(userId);
        Date nowDate = new Date();
        sysRole.setCreateTime(nowDate);
        sysRole.setUpdateTime(nowDate);
        int result = sysRoleRepository.addRole(sysRole);

        return result == 1 ? ReturnResult.OK() : ReturnResult.error("添加失败");

    }

    @Override
    public ReturnResult getRoleById(SysRoleDto sysRoleDto) {
        SysRole sysRole = sysRoleRepository.getRoleById(sysRoleDto.getId());
        SysRoleVo sysRoleVo = msSysRoleMapper.sysRoleToSysRoleVo(sysRole);
        return ReturnResult.OK(sysRoleVo);
    }

    @Override
    public ReturnResult updateRole(SysRoleDto sysRoleDto) {
        Long userId = SecurityUserUtils.getCurrentUserId();
        SysRole sysRole = msSysRoleMapper.sysRoleDtoToSysRole(sysRoleDto);
        SysRole oldSysRole = sysRoleRepository.selectById(sysRole.getId());
        sysRole.setCreater(oldSysRole.getCreater());
        sysRole.setCreateTime(oldSysRole.getCreateTime());
        sysRole.setUpdater(userId);
        sysRole.setUpdateTime(new Date());
        int result = sysRoleRepository.updateRole(sysRole);
        return ReturnResult.OK();
    }

    /**
     * 删除角色
     * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnResult deleteRole(SysRoleDto sysRoleDto) {
        Long roleId = sysRoleDto.getId();
        int result = sysRoleRepository.deleteById(roleId);
        if (result == 0) {
            return ReturnResult.error("删除失败，角色不存在");
        }
        //同时删除角色的菜单权限
        roleMenuService.deleteByRoleId(roleId);
        return ReturnResult.OK("删除成功");

    }

    @Override
    public ReturnResult getRoleSelectOptions() {
        // 获取所有角色
        List<SysRole> sysRoles = sysRoleRepository.selectList();
        //排除掉超级管理员后返回所有角色
        //暂时全给，添加的时候判断不能添加超级管理员
//        List<SysRole> noSuperSysRoles = sysRoles.stream().filter(sysRole -> !sysRole.getId().equals(1L)).collect(Collectors.toList());
        List<SelectOptionVO> roleSelectOptions = new ArrayList<>();
        for (SysRole sysRole : sysRoles) {
            SelectOptionVO selectOptionVO = new SelectOptionVO();
            selectOptionVO.setLabel(sysRole.getRoleName());
            selectOptionVO.setValue( Long.toString(sysRole.getId()));
            selectOptionVO.setKey( Long.toString(sysRole.getId()));
            roleSelectOptions.add(selectOptionVO);
        }

        return ReturnResult.OK(roleSelectOptions);
    }

    @Override
    public List<String> getRolesById(List<Long> roleIds) {
      List<SysRole>  roles  = sysRoleRepository.selectByIds(roleIds);
      return roles.stream().map(sysRole->sysRole.getRole()).collect(Collectors.toList());
    }

    @Override
    public Map<Long, String> getRoleMap() {
        return sysRoleRepository.selectList()
                .stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName));

    }
    /**
     * 根据角色id获取角色详情
     * */
    @Override
    public ReturnResult getRoleDetailById(Long roleId) {
        SysRole sysRole = sysRoleRepository.selectById(roleId);
        SysRoleVo sysRoleVo = msSysRoleMapper.sysRoleToSysRoleVo(sysRole);
        supplementSysRoleVoSomeFields(sysRoleVo);
        return ReturnResult.OK(sysRoleVo);
    }
    /**
     * 补充SysRoleVo某些字段值
     * **/
    private void supplementSysRoleVoSomeFields(SysRoleVo sysRoleVo) {
        //设置setStatusStr
        String status = sysRoleVo.getStatus();
        if ("1".equals(status)) {
            sysRoleVo.setStatusStr("正常");
        } else if ("0".equals(status)) {
            sysRoleVo.setStatusStr("禁用");
        }
    }
}




