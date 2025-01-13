package com.limelight.zerotier.model;

import com.limelight.zerotier.db.ZTDatabase;
import com.limelight.zerotier.model.type.NetworkStatus;
import com.limelight.zerotier.model.type.NetworkType;

import java.util.List;

import lombok.Data;

@Data
public class NetworkConfig {
    private Long id;

    private NetworkType type;

    private NetworkStatus status;

    private String mac;

    private String mtu;

    private boolean broadcast;

    private boolean bridging;

    private boolean routeViaZeroTier;

    private boolean useCustomDNS;

    private int dnsMode;

    private List<AssignedAddress> assignedAddresses;

    private List<DnsServer> dnsServers;

    public NetworkConfig(Long id, NetworkType type, NetworkStatus status, String mac, String mtu,
                         boolean broadcast, boolean bridging, boolean routeViaZeroTier, boolean useCustomDNS, int dnsMode) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.mac = mac;
        this.mtu = mtu;
        this.broadcast = broadcast;
        this.bridging = bridging;
        this.routeViaZeroTier = routeViaZeroTier;
        this.useCustomDNS = useCustomDNS;
        this.dnsMode = dnsMode;
    }

    public NetworkConfig(Long id, boolean routeViaZeroTier, int dnsMode) {
        this.id = id;
        this.routeViaZeroTier = routeViaZeroTier;
        this.dnsMode = dnsMode;
    }

    public NetworkConfig() {
    }




    public List<AssignedAddress> getAssignedAddresses() {
        return ZTDatabase.getInstance(null).getAssignedAddresses(id);
    }


    public synchronized void resetAssignedAddresses() {
        assignedAddresses = null;
    }

    public void setAssignedAddresses(List<AssignedAddress> assignedAddresses) {
        this.assignedAddresses = assignedAddresses;
        if (assignedAddresses != null) {
            ZTDatabase.getInstance(null).saveAssignedAddresses(id, assignedAddresses);
        }
    }


    public List<DnsServer> getDnsServers() {
        return ZTDatabase.getInstance(null).getDnsServers(id);
    }

    public synchronized void resetDnsServers() {
        dnsServers = null;
    }

    public void setDnsServers(List<DnsServer> dnsServers) {
        this.dnsServers = dnsServers;
        if (dnsServers != null) {
            ZTDatabase.getInstance(null).saveDnsServers(id, dnsServers);
        }
    }


    public void delete() {
        ZTDatabase.getInstance(null).deleteNetworkConfig(this);
    }


    public void refresh() {
        NetworkConfig refreshed = ZTDatabase.getInstance(null).getNetworkConfig(this.id);
        if (refreshed == null) {
            throw new RuntimeException("Entity does not exist in the database anymore: NetworkConfig with key " + id);
        }
        this.routeViaZeroTier = refreshed.isRouteViaZeroTier();
        this.dnsMode = refreshed.getDnsMode();
        this.type = refreshed.getType();
        this.status = refreshed.getStatus();
        this.mac = refreshed.getMac();
        this.mtu = refreshed.getMtu();
        this.broadcast = refreshed.isBroadcast();
        this.bridging = refreshed.isBridging();
        this.dnsServers = refreshed.getDnsServers();
    }

    public void update() {
        ZTDatabase.getInstance(null).saveNetworkConfig(this);
    }




}