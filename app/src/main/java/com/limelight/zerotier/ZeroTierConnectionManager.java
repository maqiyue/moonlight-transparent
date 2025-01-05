package com.limelight.zerotier;


import com.zerotier.sockets.ZeroTierEventListener;
import com.zerotier.sockets.ZeroTierNative;
import com.zerotier.sockets.ZeroTierNode;

import java.math.BigInteger;
import java.net.InetAddress;
/**
 * ZeroTier连接管理器
 * 提供连接、断开、自动重连等功能
 */
public class ZeroTierConnectionManager {
    // 单例
    private static ZeroTierConnectionManager instance;

    /** 最大重试次数 */
    private static final int MAX_RETRY_COUNT = 3;
    /** 重试间隔(毫秒) */
    private static final long RETRY_INTERVAL = 5000;
    /** 等待网络就绪的超时时间(毫秒) */
    private static final long NETWORK_READY_TIMEOUT = 10000;

    private ZeroTierNode node;
    private String storagePath;
    private long currentNetworkId;
    private boolean isConnected = false;
    private int retryCount = 0;
    private boolean isRetrying = false;
    /** 是否启用自动重连 */
    private boolean autoReconnectEnabled = false;

    public interface StateCallback {
        void onStateChanged(boolean connected);
    }
    
    private StateCallback stateCallback;
    
    public void setStateCallback(StateCallback callback) {
        this.stateCallback = callback;
    }

    private void updateConnectionState(boolean connected) {
        isConnected = connected;
        if (stateCallback != null) {
            stateCallback.onStateChanged(connected);
        }
    }

    private ZeroTierConnectionManager() {
        try {
            node = new ZeroTierNode();
        } catch (ExceptionInInitializerError e) {
            System.out.println("ZeroTier 初始化失败: " + e.getMessage());
            // 可以在这里添加错误处理逻辑
            node = null;
        }
    }

    public static ZeroTierConnectionManager getInstance() {
        if (instance == null) {
            instance = new ZeroTierConnectionManager();
        }
        return instance;
    }

    /**
     * 初始化ZeroTier节点
     *
     * @param storagePath 存储路径
     */
    public void init(String storagePath) {
        if (node == null) {
            System.out.println("ZeroTier 节点未初始化");
            return;
        }

        this.storagePath = storagePath;
        try {
            node.initFromStorage(storagePath);
            node.initSetEventHandler(new ConnectionEventListener());
        } catch (Exception e) {
            System.out.println("连接失败: 初始化异常 - " + e.getMessage());
        }
    }

    /**
     * 连接到指定网络
     *
     * @param networkIdHex 16进制的网络ID
     * @return 是否连接成功
     */
    public boolean connect(String networkIdHex) {
        if (node == null) {
            System.out.println("ZeroTier 节点未初始化");
            return false;
        }

        if (isConnected) {
            return true;
        }

        try {
            // 转换网络ID
            currentNetworkId = new BigInteger(networkIdHex, 16).longValue();

            // 启动节点
            node.start();

            // 等待节点上线
            long startTime = System.currentTimeMillis();
            while (!node.isOnline()) {
                if (System.currentTimeMillis() - startTime > NETWORK_READY_TIMEOUT) {
                    System.out.println("连接失败: 节点上线超时");
                    return false;
                }
                Thread.sleep(100);
            }

            // 加入网络
            node.join(currentNetworkId);

            // 等待网络就绪
            startTime = System.currentTimeMillis();
            while (!node.isNetworkTransportReady(currentNetworkId)) {
                if (System.currentTimeMillis() - startTime > NETWORK_READY_TIMEOUT) {
                    System.out.println("连接失败: 网络就绪超时");
                    return false;
                }
                Thread.sleep(100);
            }

            isConnected = true;
            updateConnectionState(true);
            InetAddress addr4 = node.getIPv4Address(currentNetworkId);
            System.out.println("连接成功: " + addr4.getHostAddress());

            // 连接成功后才启用自动重连
            autoReconnectEnabled = true;
            return true;

        } catch (Exception e) {
            System.out.println("连接失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 断开当前网络连接
     */
    public void disconnect() {
        if (!isConnected) {
            return;
        }

        try {
            // 禁用自动重连
            autoReconnectEnabled = false;

            node.leave(currentNetworkId);
            node.stop();
            isConnected = false;
            updateConnectionState(false);
            System.out.println("连接断开");
        } catch (Exception e) {
            System.out.println("连接断开失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前连接状态
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * 开始重连流程
     */
    private void startReconnection() {
        if (isRetrying || !autoReconnectEnabled) { // 添加自动重连检查
            return;
        }

        isRetrying = true;
        retryCount = 0;

        new Thread(() -> {
            while (retryCount < MAX_RETRY_COUNT && !isConnected && autoReconnectEnabled) { // 添加自动重连检查
                try {
                    retryCount++;
                    if (connect(Long.toHexString(currentNetworkId))) {
                        break;
                    }
                    Thread.sleep(RETRY_INTERVAL);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (!isConnected && autoReconnectEnabled) { // 添加自动重连检查
                System.out.println("连接失败: 重试次数已用完");
            }
            isRetrying = false;
        }).start();
    }

    /**
     * 简化的事件监听器
     */
    private class ConnectionEventListener implements ZeroTierEventListener {
        @Override
        public void onZeroTierEvent(long id, int eventCode) {
            // 只处理当前网络的事件
            if (id != currentNetworkId && eventCode != ZeroTierNative.ZTS_EVENT_NODE_OFFLINE) {
                return;
            }

            // 使用 if-else 替代 switch-case
            if (eventCode == ZeroTierNative.ZTS_EVENT_NODE_OFFLINE) {
                if (isConnected && autoReconnectEnabled) {
                    updateConnectionState(false);
                    System.out.println("连接断开: 节点离线");
                    startReconnection();
                }
            } else if (eventCode == ZeroTierNative.ZTS_EVENT_NETWORK_DOWN) {
                if (isConnected && autoReconnectEnabled) {
                    updateConnectionState(false);
                    System.out.println("连接断开: 网络关闭");
                    startReconnection();
                }
            } else if (eventCode == ZeroTierNative.ZTS_EVENT_NETWORK_READY_IP4) {
                isConnected = true;
                try {
                    InetAddress addr4 = node.getIPv4Address(currentNetworkId);
                    System.out.println("网络就绪: " + addr4.getHostAddress());
                } catch (Exception e) {
                    System.out.println("网络就绪");
                }
            } else if (eventCode == ZeroTierNative.ZTS_EVENT_NETWORK_ACCESS_DENIED) {
                isConnected = false;
                System.out.println("连接失败: 网络访问被拒绝");
            } else if (eventCode == ZeroTierNative.ZTS_EVENT_NETWORK_NOT_FOUND) {
                isConnected = false;
                System.out.println("连接失败: 网络未找到");
            }
        }
    }
}
