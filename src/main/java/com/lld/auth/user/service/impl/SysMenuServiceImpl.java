package com.lld.auth.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lld.auth.security.entity.MyUsernamePasswordAuthenticationToken;
import com.lld.auth.user.entity.DTO.SysMenuDto;
import com.lld.auth.user.entity.MenuMeta;
import com.lld.auth.user.entity.SysMenu;
import com.lld.auth.user.entity.VO.SysMenuVo;
import com.lld.auth.user.entity.WebMenu;
import com.lld.auth.user.mapstruct.MSWebMenuMapper;
import com.lld.auth.user.service.SysMenuService;
import com.lld.auth.user.mapper.SysMenuMapper;
import com.lld.saltedfishutils.entity.WebComponentVO.CascaderVO;
import com.lld.saltedfishutils.entity.WebComponentVO.TreeNodeVo;
import com.lld.saltedfishutils.utils.ReturnResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author saltedFish
* @description 针对表【sys_menu】的数据库操作Service实现
* @createDate 2024-09-17 02:49:00
*/
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu>
    implements SysMenuService{
    MSWebMenuMapper msWebMenuMapper = MSWebMenuMapper.INSTANCE;
    @Override
    public List<WebMenu> getUserMenus() {
        List<SysMenu> userMenus = baseMapper.selectList(null);
        List<WebMenu> webMenus = buildtUserMenus(userMenus, null, 5);
        return webMenus;
    }

    @Override
    public ReturnResult getMenulist() {
        List<SysMenu> userMenus = baseMapper.selectList(null);
        List<SysMenuVo> sysMenuVos = buildList(userMenus, null, 5);
        return ReturnResult.OK(sysMenuVos);
    }

    @Override
    public ReturnResult getParentTree() {
        List<SysMenu> userMenus = baseMapper.selectList(null);
        List<TreeNodeVo> cascaderVOList = buildParentTree(userMenus, null, 5);
        return ReturnResult.OK(cascaderVOList);
    }

    @Override
    public ReturnResult addMenu(SysMenuDto sysMenuDto) {
        MyUsernamePasswordAuthenticationToken authentication = (MyUsernamePasswordAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        Long userId = authentication.getUserId();
        if(userId.equals(1L)){
            SysMenu sysMenu = msWebMenuMapper.sysMenuDtoToSysMenu(sysMenuDto);
            Date now = new Date();
            sysMenu.setCreateTime(now);
            sysMenu.setUpdateTime(now);

            sysMenu.setCreater(userId);
            sysMenu.setUpdater(userId);
            if(sysMenu.getMenuType().equals("0")){
                sysMenu.setComponent("LAYOUT");
            }
            baseMapper.insert(sysMenu);
            System.out.println( sysMenu);
            return ReturnResult.OK();
        }
        return ReturnResult.error("权限不足");

    }

    @Override
    public ReturnResult getMenuById(Long id) {
        SysMenu sysMenu = baseMapper.selectById(id);
        if(sysMenu!=null){
            SysMenuVo sysMenuVo = msWebMenuMapper.sysMenuToSysMenuVo(sysMenu);
            return ReturnResult.OK(sysMenuVo);
        }
        return ReturnResult.error("菜单不存在");
    }

    @Override
    public ReturnResult updateMenu(SysMenuDto sysMenuDto) {
        MyUsernamePasswordAuthenticationToken authentication = (MyUsernamePasswordAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        Long userId = authentication.getUserId();
        if(!userId.equals(1L)){
            ReturnResult.error("此用户没有权限更改菜单");
        }
        SysMenu sysMenu = msWebMenuMapper.SysMenuDtoToSysMenu(sysMenuDto);
        Date now = new Date();
        sysMenu.setUpdateTime(now);
        sysMenu.setUpdater(userId);
        if(sysMenu.getMenuType().equals("0")){
            sysMenu.setComponent("LAYOUT");
        }
        int i = baseMapper.updateMenuById(sysMenu);
        if(i>0){
            return ReturnResult.OK("更新成功");
        }
        return ReturnResult.error("未找到记录");
    }

    @Override
    public ReturnResult deleteMenuById(SysMenuDto sysMenuDto) {
        MyUsernamePasswordAuthenticationToken authentication = (MyUsernamePasswordAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        Long userId = authentication.getUserId();
        if(!userId.equals(1L)){
            ReturnResult.error("此用户没有权限删除菜单");
        }
        int i = baseMapper.deleteById(sysMenuDto.getId());
        if(i>0){
            return ReturnResult.OK("删除成功");
        }
        return ReturnResult.error("未找到记录");

    }

    private List<TreeNodeVo> buildParentTree(List<SysMenu> userMenus, SysMenu parent, Integer depth) {
        Integer tenmpDepthdepth = depth-1;
        if (tenmpDepthdepth == 0) return new ArrayList<>();
        List<SysMenu> collect = new ArrayList<>();
        for (SysMenu userMenu : userMenus) {
            Long parentId = userMenu.getParentId();
            Long prevId = parent == null? null:parent.getId();
            if(prevId==null&&parentId==null){
                collect.add(userMenu);
            }else if(prevId!=null&&prevId.equals(parentId)){
                collect.add(userMenu);
            }
        }

        collect.sort(Comparator.comparing(o -> o.getOrderNo()));

        List<TreeNodeVo> treeNodeVoList = new ArrayList<>();
        for (SysMenu sysMenu : collect) {
            TreeNodeVo cascaderVO = new TreeNodeVo();
            cascaderVO.setLabel(sysMenu.getTitle());
            cascaderVO.setValue(sysMenu.getId());
            List<TreeNodeVo> childs = buildParentTree(userMenus, sysMenu,tenmpDepthdepth);
            cascaderVO.setChildren(childs);
            treeNodeVoList.add(cascaderVO);
        }
        return treeNodeVoList;

    }

    private List<SysMenuVo> buildList(List<SysMenu> userMenus,SysMenu parent,Integer depth){
        Integer tenmpDepthdepth = depth-1;
        if (tenmpDepthdepth == 0) return new ArrayList<>();
        List<SysMenu> collect = new ArrayList<>();
        for (SysMenu userMenu : userMenus) {
            Long parentId = userMenu.getParentId();
            Long prevId = parent == null? null:parent.getId();
            if(prevId==null&&parentId==null){
                collect.add(userMenu);
            }else if(prevId!=null&&prevId.equals(parentId)){
                collect.add(userMenu);
            }
        }

        List<SysMenuVo> sysMenuVos = new ArrayList<>();
        for (SysMenu sysMenu : collect) {
            List<SysMenuVo> childs = buildList(userMenus, sysMenu,tenmpDepthdepth);
            SysMenuVo sysMenuVo = msWebMenuMapper.sysMenuToSysMenuVo(sysMenu);
            String menuType = sysMenuVo.getMenuType();
            if(menuType.equals("0")){
                sysMenuVo.setMenuTypeStr("目录");
            }else if(menuType.equals("1")){
                sysMenuVo.setMenuTypeStr("菜单");
            }else if(menuType.equals("2")){
                sysMenuVo.setMenuTypeStr("按钮");
            }
            sysMenuVo.setChildren(childs);
            sysMenuVos.add(sysMenuVo);
        }
        sysMenuVos.sort(Comparator.comparing(o -> o.getOrderNo()));
        return sysMenuVos;

    }



    private List<WebMenu> buildtUserMenus(List<SysMenu> userMenus,SysMenu parent,Integer depth){
        Integer tenmpDepthdepth = depth-1;
        if (tenmpDepthdepth == 0) return new ArrayList<>();
        List<SysMenu> collect = new ArrayList<>();
        for (SysMenu userMenu : userMenus) {
            Long parentId = userMenu.getParentId();
            Long prevId = parent == null? null:parent.getId();
            if(prevId==null&&parentId==null){
                collect.add(userMenu);
            }else if(prevId!=null&&prevId.equals(parentId)){
                collect.add(userMenu);
            }
        }


        List<WebMenu> webMenus = new ArrayList<>();
        for (SysMenu sysMenu : collect) {
            WebMenu webMenu = msWebMenuMapper.toWebMenu(sysMenu);
            MenuMeta menuMeta = msWebMenuMapper.toMenuMeta(sysMenu);
            webMenu.setMeta(menuMeta);
            List<WebMenu> childs = buildtUserMenus(userMenus, sysMenu,tenmpDepthdepth);
            webMenu.setChildren(childs);
            webMenus.add(webMenu);
        }
        webMenus.sort(Comparator.comparing(o -> o.getMeta().getOrderNo()));
        return webMenus;
    }

}




