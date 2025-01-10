package com.limelight.zerotier.model;

import lombok.Data;

@Data
public class AssignedAddress {

    private Long id;
    private long networkId;
    private AddressType type;
    private byte[] addressBytes;
    private String addressString;
    private short prefix;

    public enum AddressType {
        UNKNOWN(0),
        IPV4(1),
        IPV6(2);
        final int id;
        AddressType(int i) {
            this.id = i;
        }

        public String toString() {
            int i = this.id;
            if (i != 1) {
                return i != 2 ? "Unknown" : "IPv6";
            }
            return "IPv4";
        }
    }
}
