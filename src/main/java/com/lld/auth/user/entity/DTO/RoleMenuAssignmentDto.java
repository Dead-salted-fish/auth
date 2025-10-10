package com.lld.auth.user.entity.DTO;

import lombok.Data;

import java.util.List;
@Data
public class RoleMenuAssignmentDto {
    private Long roleId;
    private String role;
    private List<Long> menuIds;
    

}