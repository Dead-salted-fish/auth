package com.lld.auth.user.service.impl;

import com.lld.auth.user.entity.DTO.RoleMenuAssignmentDto;
import com.lld.auth.user.entity.RoleMenu;
import com.lld.auth.user.entity.SysMenu;
import com.lld.auth.user.repository.RoleMenuRepository;
import com.lld.auth.user.service.RoleMenuService;
import com.lld.auth.user.service.SysMenuService;
import com.lld.auth.utils.SecurityUserUtils;
import com.lld.saltedfishutils.entity.WebComponentVO.TreeTreeNodeVo;
import com.lld.saltedfishutils.web.result.ReturnResult;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author saltedFish
 * @description 针对表【role_menu】的数据库操作Service实现
 * @createDate 2025-07-02 22:06:29
 */
@Service
public class RoleMenuServiceImpl implements RoleMenuService {

    private final RoleMenuRepository roleMenuRepository;

    private final SysMenuService sysMenuService;

    public RoleMenuServiceImpl(RoleMenuRepository roleMenuRepository, @Lazy SysMenuService sysMenuService) {
        this.roleMenuRepository = roleMenuRepository;
        this.sysMenuService = sysMenuService;
    }

    /**
     * 更新用户角色菜单关系
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnResult updateRoleMenu(RoleMenuAssignmentDto roleMenuAssignmentDto) {

        String role = roleMenuAssignmentDto.getRole();
        Long roleId = roleMenuAssignmentDto.getRoleId();
        List<Long> menuIds = roleMenuAssignmentDto.getMenuIds();
        //获取当前用户ID
        Long currentUserId = SecurityUserUtils.getCurrentUserId();

        //拿到菜单id和菜单标题
        List<SysMenu> menuByIds = sysMenuService.getMenuByIds(menuIds);
        Map<Long, SysMenu> collect = menuByIds.stream().collect(Collectors.toMap(SysMenu::getId, menu -> menu));

        //构建角色菜单关系
        List<RoleMenu> roleMenus = new ArrayList<>();
        for (Long menuId : menuIds) {
            RoleMenu roleMenu = new RoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setRole(role);
            roleMenu.setMenuId(menuId);
            roleMenu.setMenuTitle(collect.get(menuId).getTitle());
            roleMenu.setMenuType(collect.get(menuId).getMenuType());
            Date now = new Date();
            roleMenu.setCreateTime(now);
            roleMenu.setUpdateTime(now);
            roleMenu.setCreater(currentUserId);
            roleMenu.setUpdater(currentUserId);
            roleMenus.add(roleMenu);
        }
        System.out.println(roleMenus);
        int deleteCount = roleMenuRepository.deleteByRoleId(roleId);
        int insertCount = roleMenuRepository.insertRoleMenus(roleMenus);

        return ReturnResult.OK();
    }

    @Override
    public List<RoleMenu> getMenuIdsByRoleId(Long roleId) {
        List<RoleMenu> menus = roleMenuRepository.selectByRoleId(roleId);
        
        return menus;
    }
    
    @Override
    public ReturnResult getRoleMenuIdsGroupedByType(Long roleId) {
        List<RoleMenu> roleMenus = getMenuIdsByRoleId(roleId);
        List<Long> childMenuIds = roleMenus.stream()
                .filter(roleMenu -> "1".equals(roleMenu.getMenuType()))
                .map(RoleMenu::getMenuId)
                .collect(Collectors.toList());

        List<Long> parentMenuIds = roleMenus.stream()
                .filter(roleMenu -> "0".equals(roleMenu.getMenuType()))
                .map(RoleMenu::getMenuId)
                .collect(Collectors.toList());
        Map<String, List< Long>> menuByIds =new HashMap<>();
        menuByIds.put("childMenus",childMenuIds);
        menuByIds.put("parentMenus",parentMenuIds);
        return ReturnResult.OK(menuByIds);
    }
    /**
     * 获取角色实际拥有权限的拥有的菜单树
     * **/
    @Override
    public ReturnResult getRoleOwnedMenuTreeByRoleId(Long roleId) {
        List<RoleMenu> roleMenus = roleMenuRepository.selectByRoleId(roleId);
        List<Long> menuIdsCollect = roleMenus.stream().map(roleMenu -> roleMenu.getMenuId()).collect(Collectors.toList());
        //拿到拥有所有菜单的菜单树
        ReturnResult tempData = sysMenuService.getMenuTree();
        List<TreeTreeNodeVo> menuTree = (List<TreeTreeNodeVo>)tempData.getData();
        //根据角色拥有的菜单id进行过滤
        filterMenuTree(menuTree,menuIdsCollect);
        return ReturnResult.OK(menuTree);
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        roleMenuRepository.deleteByRoleId(roleId);
    }

    /**
     * 递归过滤菜单树，筛选出角色实际拥有权限的菜单
     * **/
    private void filterMenuTree(List<TreeTreeNodeVo> allMenuTree, List<Long> menuIdsCollect) {
        Iterator<TreeTreeNodeVo> iterator = allMenuTree.iterator();
        while (iterator.hasNext()) {
            TreeTreeNodeVo node = iterator.next();

            // 先处理子节点
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                filterMenuTree(node.getChildren(), menuIdsCollect);
            }

            // 检查当前节点是否需要保留
            if ( !menuIdsCollect.contains((Long) node.getKey())) {
                iterator.remove();
            }
        }
    }
}




