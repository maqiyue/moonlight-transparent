package com.limelight.zerotier.model;



import com.limelight.zerotier.db.ZTDatabase;

import org.jetbrains.annotations.NotNull;

import lombok.Data;

@Data
public class Network {
    private Long networkId;

    private String networkIdStr;

    private String networkName;

    private boolean useDefaultRoute;

    private boolean lastActivated;

    private long networkConfigId;

    private boolean connected;

    private NetworkConfig networkConfig;

    private transient Long networkConfig__resolvedKey;

    public void setNetworkConfig(@NotNull NetworkConfig networkConfig) {
        if (networkConfig == null) {
            throw new RuntimeException(
                    "To-one property 'networkConfigId' has not-null constraint; cannot set to-one to null");
        }
        synchronized (this) {
            this.networkConfig = networkConfig;
            networkConfigId = networkConfig.getId();
            networkConfig__resolvedKey = networkConfigId;
        }
    }

    public void delete() {
        ZTDatabase.getInstance(null).deleteNetwork(this);
    }


    public void refresh() {
        Network refreshed = ZTDatabase.getInstance(null).getNetworkById(this.networkId);
        if (refreshed == null) {
            throw new RuntimeException("Entity does not exist in the database anymore: Network with key " + networkId);
        }
        this.networkName = refreshed.getNetworkName();
        this.networkIdStr = refreshed.getNetworkIdStr();
        this.lastActivated = refreshed.isLastActivated();
        this.networkConfigId = refreshed.getNetworkConfigId();
        this.networkConfig = refreshed.getNetworkConfig();
    }


    public void update() {
        ZTDatabase.getInstance(null).saveNetwork(this);
    }

}