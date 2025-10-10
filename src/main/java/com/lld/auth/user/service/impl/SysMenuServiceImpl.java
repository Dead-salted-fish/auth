package com.lld.auth.user.service.impl;

import com.lld.auth.user.entity.DTO.SysMenuDto;
import com.lld.auth.user.entity.MenuMeta;
import com.lld.auth.user.entity.SysMenu;
import com.lld.auth.user.entity.VO.SysMenuVo;
import com.lld.auth.user.entity.WebMenu;
import com.lld.auth.user.mapstruct.MSSysMenuMapper;
import com.lld.auth.user.mapstruct.MSWebMenuMapper;
import com.lld.auth.user.repository.SysMenuRepository;
import com.lld.auth.user.service.SysMenuService;
import com.lld.auth.utils.SecurityUserUtils;
import com.lld.saltedfishutils.entity.WebComponentVO.TreeNodeVo;
import com.lld.saltedfishutils.entity.WebComponentVO.TreeTreeNodeVo;
import com.lld.saltedfishutils.utils.ReturnResult;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
* @author saltedFish
* @description 针对表【sys_menu】的数据库操作Service实现
* @createDate 2024-09-17 02:49:00
*/
@Service
public class    SysMenuServiceImpl implements SysMenuService{

    private static final String MENU_TYPE_DIRECTORY = "0";
    private static final String LAYOUT_COMPONENT = "LAYOUT";

    MSWebMenuMapper msWebMenuMapper = MSWebMenuMapper.INSTANCE;
    MSSysMenuMapper mSSysMenuMapper = MSSysMenuMapper.INSTANCE;

    private final SysMenuRepository sysMenuRepository;

    public SysMenuServiceImpl(SysMenuRepository sysMenuRepository) {
        this.sysMenuRepository = sysMenuRepository;
    }

    /***
     * 获取用户的菜单树 用于 前端生成菜单
     **/
    @Override
    public List<WebMenu> getUserMenus() {
        // 获取用户菜单并且分组
        List<SysMenu> userMenus = sysMenuRepository.selectList(null);
        if (userMenus == null || userMenus.isEmpty()) return new ArrayList<>();


        Map<Long, List<SysMenu>> menuMap = extractMenuMap(userMenus);

        // 返回构建菜单树
        return buildUserMenus(menuMap, null, 5);
    }

    private List<WebMenu> buildUserMenus(Map<Long, List<SysMenu>> menuMap,SysMenu parent,Integer depth){
        if (depth <= 0) return new ArrayList<>();

        // 获取当前层级的子菜单
        List<SysMenu> currentMenus = menuMap.get(parent == null ? null : parent.getId());
        if (currentMenus == null || currentMenus.isEmpty()) return new ArrayList<>();

        currentMenus.sort(Comparator.comparingInt(SysMenu::getOrderNo));

        // 转换为WebMenu并递归处理子节点
        List<WebMenu> webMenus = new ArrayList<>();
        for (SysMenu menu : currentMenus) {
            // 过滤掉禁用的菜单
            if(menu.getStatus().equals("0")){
                continue;
            }

            WebMenu webMenu = msWebMenuMapper.toWebMenu(menu);
            MenuMeta meta = msWebMenuMapper.toMenuMeta(menu);
            webMenu.setMeta(meta);


            List<WebMenu> children = buildUserMenus(menuMap, menu, depth - 1);
            webMenu.setChildren(children);
            webMenus.add(webMenu);
        }

        return webMenus;
    }
    /**
    *根据父菜单id对菜单进行分组
     **/
    private Map<Long, List<SysMenu>> extractMenuMap(List<SysMenu> userMenus) {
        // 先分组非null键，再单独处理null键
        // 在部分 Java 版本或实现中，Collectors.groupingBy(Function) 默认不允许 null 作为键
        Map<Long, List<SysMenu>> menuMap = userMenus.stream()
                .filter(menu -> menu.getParentId() != null)
                .collect(Collectors.groupingBy(SysMenu::getParentId));

        // 单独处理parentId=null的根菜单
        List<SysMenu> rootMenus = userMenus.stream()
                .filter(menu -> menu.getParentId() == null)
                .collect(Collectors.toList());
        // 手动添加null键
        menuMap.put(null, rootMenus);

        return menuMap;
    }

    /**
     * 获取所有菜单，没有任何菜单过滤操作
     * */
    @Override
    public ReturnResult getMenulist() {
        List<SysMenu> userMenus = sysMenuRepository.selectList(null);
        Map<Long, List<SysMenu>> menumap = extractMenuMap(userMenus);

        List<SysMenuVo> sysMenuVos = buildList(menumap, null, 5);

        return ReturnResult.OK(sysMenuVos);
    }

    private List<SysMenuVo> buildList(Map<Long, List<SysMenu>> menuMap,SysMenu parent,Integer depth){
        if (depth <= 0) return new ArrayList<>();

        // 获取当前层级的子菜单
        List<SysMenu> currentMenus = menuMap.get(parent == null ? null : parent.getId());
        if (currentMenus == null || currentMenus.isEmpty()) return new ArrayList<>();
        currentMenus.sort(Comparator.comparingInt(SysMenu::getOrderNo));

        List<SysMenuVo> sysMenuVos = new ArrayList<>();
        for (SysMenu sysMenu : currentMenus) {

            SysMenuVo sysMenuVo = mSSysMenuMapper.sysMenuToSysMenuVo(sysMenu);
            supplementSysMenuVoSomeField(sysMenuVo);

            List<SysMenuVo> childs = buildList(menuMap, sysMenu,depth-1);
            sysMenuVo.setChildren(childs);
            sysMenuVos.add(sysMenuVo);
        }

        return sysMenuVos;

    }

    private void supplementSysMenuVoSomeField(SysMenuVo sysMenuVo){
        String menuType = sysMenuVo.getMenuType();
        String status = sysMenuVo.getStatus();

        if(status.equals("0")){
            sysMenuVo.setStatusStr("禁用");
        }else if(status.equals("1")){
            sysMenuVo.setStatusStr("正常");
        }

        if(menuType.equals("0")){
            sysMenuVo.setMenuTypeStr("目录");
        }else if(menuType.equals("1")){
            sysMenuVo.setMenuTypeStr("菜单");
        }else if(menuType.equals("2")){
            sysMenuVo.setMenuTypeStr("按钮");
        }
    }

    /**
     * 获取父菜单树，selectoption 树形结构
     * */
    @Override
    public ReturnResult getParentTree() {

        List<SysMenu> userMenus = sysMenuRepository.selectList(null);
        Map<Long, List<SysMenu>> menuMap = extractMenuMap(userMenus);
        List<TreeNodeVo> cascaderVOList = buildParentTree(menuMap, null, 5);

        return ReturnResult.OK(cascaderVOList);
    }

    private List<TreeNodeVo> buildParentTree(Map<Long, List<SysMenu>> menuMap, SysMenu parent, Integer depth) {

        if (depth <= 0) return new ArrayList<>();

        // 获取当前层级的子菜单
        List<SysMenu> currentMenus = menuMap.get(parent == null ? null : parent.getId());
        if (currentMenus == null || currentMenus.isEmpty()) return new ArrayList<>();
        currentMenus.sort(Comparator.comparingInt(SysMenu::getOrderNo));

        //构建树形结构
        List<TreeNodeVo> treeNodeVoList = new ArrayList<>();
        for (SysMenu sysMenu : currentMenus) {
            TreeNodeVo treeNodeVo = new TreeNodeVo();
            treeNodeVo.setLabel(sysMenu.getTitle());
            treeNodeVo.setValue(sysMenu.getId());
            List<TreeNodeVo> childs = buildParentTree(menuMap, sysMenu,depth-1);
            treeNodeVo.setChildren(childs);
            treeNodeVoList.add(treeNodeVo);
        }
        return treeNodeVoList;

    }


    /**
     * 添加菜单
     **/
    @Override
    public ReturnResult addMenu(SysMenuDto sysMenuDto) {
        Long userId = SecurityUserUtils.getCurrentUserId();

        SysMenu sysMenu = mSSysMenuMapper.sysMenuDtoToSysMenu(sysMenuDto);
        Date now = new Date();
        sysMenu.setCreateTime(now);
        sysMenu.setUpdateTime(now);

        sysMenu.setCreater(userId);
        sysMenu.setUpdater(userId);
        if(MENU_TYPE_DIRECTORY.equals(sysMenu.getMenuType())){
            sysMenu.setComponent(LAYOUT_COMPONENT);
        }
        sysMenuRepository.insert(sysMenu);
        System.out.println( sysMenu);
        return ReturnResult.OK();

    }

    /**
     * 根据菜单id获取菜单信息
     * **/
    @Override
    public ReturnResult getMenuById(Long id) {
        SysMenu sysMenu = sysMenuRepository.selectById(id);
        if(sysMenu!=null){
            SysMenuVo sysMenuVo = mSSysMenuMapper.sysMenuToSysMenuVo(sysMenu);
            return ReturnResult.OK(sysMenuVo);
        }
        return ReturnResult.error("菜单不存在");
    }
    /**
     * 更新菜单信息
     * **/
    @Override
    public ReturnResult updateMenu(SysMenuDto sysMenuDto) {

        Long userId = SecurityUserUtils.getCurrentUserId();

        SysMenu sysMenu = mSSysMenuMapper.SysMenuDtoToSysMenu(sysMenuDto);
        Date now = new Date();
        sysMenu.setUpdateTime(now);
        sysMenu.setUpdater(userId);
        if(MENU_TYPE_DIRECTORY.equals(sysMenu.getMenuType())){
            sysMenu.setComponent(LAYOUT_COMPONENT);
        }
        int i = sysMenuRepository.updateMenuById(sysMenu);
        if(i>0){
            return ReturnResult.OK("更新成功");
        }
        return ReturnResult.error("未找到记录");
    }

     /**
      * 根据菜单id删除菜单
      * **/
    @Override
    public ReturnResult deleteMenuById(SysMenuDto sysMenuDto) {

        int i = sysMenuRepository.deleteById(sysMenuDto.getId());
        if(i>0){
            return ReturnResult.OK("删除成功");
        }
        return ReturnResult.error("未找到记录");

    }

    /**
     * 构建菜单树，返回树形控件(Tree) 数据结构
     **/
    @Override
    public ReturnResult getMenuTree() {
        List<SysMenu> userMenus = sysMenuRepository.selectList(null);

        if (userMenus == null || userMenus.isEmpty()) return ReturnResult.OK(new ArrayList<>());

        // 先分组非null键，再单独处理null键
        // 在部分 Java 版本或实现中，Collectors.groupingBy(Function) 默认不允许 null 作为键
        Map<Long, List<SysMenu>> menuMap = userMenus.stream()
                .filter(menu -> menu.getParentId() != null)
                .collect(Collectors.groupingBy(SysMenu::getParentId));

        // 单独处理parentId=null的根菜单
        List<SysMenu> rootMenus = userMenus.stream()
                .filter(menu -> menu.getParentId() == null)
                .collect(Collectors.toList());
        menuMap.put(null, rootMenus); // 手动添加null键
        List<TreeTreeNodeVo> treeTreeNodeVos = buildMenuTreeTreeNodes(menuMap, null, 5);
        return ReturnResult.OK(treeTreeNodeVos);
    }

    private List<TreeTreeNodeVo> buildMenuTreeTreeNodes(Map<Long, List<SysMenu>> menuMap,SysMenu parent,Integer depth){
        if (depth <= 0) return new ArrayList<>();

        // 获取当前层级的子菜单
        List<SysMenu> currentMenus = menuMap.get(parent == null ? null : parent.getId());
        if (currentMenus == null || currentMenus.isEmpty()) return new ArrayList<>();

        currentMenus.sort(Comparator.comparingInt(SysMenu::getOrderNo));

        // 转换为TreeTreeNodeVo并递归处理子节点
        List<TreeTreeNodeVo> treeTreeNodeVos = new ArrayList<>();
        for (SysMenu menu : currentMenus) {
            // 过滤掉禁用的菜单
            if(menu.getStatus().equals("0")){
                continue;
            }
            TreeTreeNodeVo treeTreeNodeVo = new TreeTreeNodeVo();
            treeTreeNodeVo.setTitle(menu.getTitle());
            treeTreeNodeVo.setKey(menu.getId());


            List<TreeTreeNodeVo> children = buildMenuTreeTreeNodes(menuMap, menu, depth - 1);
            treeTreeNodeVo.setChildren(children);
            treeTreeNodeVos.add(treeTreeNodeVo);
        }

        return treeTreeNodeVos;
    }

    /**
     * 根据菜单id列表获取菜单信息
     * **/
    @Override
    public List<SysMenu> getMenuByIds(List<Long> menuIds) {
        return sysMenuRepository.selectByIds(menuIds);
    }

    /**
     * 根据菜单id获取菜单详情
     * */
    @Override
    public ReturnResult getMenuDetailById(Long id) {
        SysMenu sysMenu = sysMenuRepository.selectById(id);
        SysMenuVo sysMenuVo = mSSysMenuMapper.sysMenuToSysMenuVo(sysMenu);
        //补充vo部分字段
        supplementSysMenuVoSomeField(sysMenuVo);
        //补充父菜单名称
        Long parentId = sysMenuVo.getParentId();
        if(parentId!=null){
            SysMenu parentMenu = sysMenuRepository.selectById(parentId);
            sysMenuVo.setParentMenuStr(parentMenu.getTitle());
        }
        return ReturnResult.OK(sysMenuVo);
    }


}




