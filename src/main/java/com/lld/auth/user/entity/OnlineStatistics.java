package com.lld.auth.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public  class OnlineStatistics {
    private long totalOnline;

    private long realOnline;

    private long totalOnlineTotal;
}
