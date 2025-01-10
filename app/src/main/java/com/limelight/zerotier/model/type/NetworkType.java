package com.limelight.zerotier.model.type;

import com.zerotier.sdk.VirtualNetworkType;

public enum NetworkType {
    UNKNOWN(0),
    PRIVATE(1),
    PUBLIC(2);

    private final int id;

    NetworkType(int i) {
        this.id = i;
    }

    public static NetworkType fromInt(int i) {
        if (i != 0) {
            if (i == 1) {
                return PUBLIC;
            }
            if (i == 2) {
                return PUBLIC;
            }
            throw new RuntimeException("Unhandled value: " + i);
        }
        return PRIVATE;
    }

    public static NetworkType fromVirtualNetworkType(VirtualNetworkType virtualNetworkType) {
        switch (virtualNetworkType) {
            case NETWORK_TYPE_PRIVATE:
                return PRIVATE;
            case NETWORK_TYPE_PUBLIC:
                return PUBLIC;
            default:
                throw new RuntimeException("Unhandled type: " + virtualNetworkType);
        }
    }


    public int toInt() {
        return this.id;
    }
}
