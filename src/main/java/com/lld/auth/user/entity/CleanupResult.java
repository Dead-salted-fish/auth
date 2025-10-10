package com.lld.auth.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public  class CleanupResult {
    private long totalRemoved;

    private List<UserCleanupDetail> details;
}
