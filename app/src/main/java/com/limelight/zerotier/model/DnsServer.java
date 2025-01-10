package com.limelight.zerotier.model;


import lombok.Data;

@Data
public class DnsServer {
    private Long id;
    private Long networkId;
    private String nameserver;
}
