package com.lld.auth.user.entity;

import lombok.Data;

import java.util.List;
@Data
public class WebMenu  {
    private String path;
    private  MenuMeta meta;
    private String component;
    private Boolean Recordable;
    private String fullPath;
    private List<WebMenu> children;
}
