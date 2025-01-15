package com.limelight.zerotier.events;

import com.zerotier.sdk.VirtualNetworkConfig;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 更新网络配置的结果事件
 */
@Data
@AllArgsConstructor
public class NetworkReconfigureEvent {
    private final boolean changed;
    private final VirtualNetworkConfig virtualNetworkConfig;
}
