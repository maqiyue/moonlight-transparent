package com.limelight.zerotier;

import android.app.Activity;
import android.net.VpnService;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.zerotier.sdk.DataStoreGetListener;
import com.zerotier.sdk.DataStorePutListener;
import com.zerotier.sdk.Event;
import com.zerotier.sdk.EventListener;
import com.zerotier.sdk.Node;
import com.zerotier.sdk.NodeStatus;
import com.zerotier.sdk.PacketSender;
import com.zerotier.sdk.Peer;
import com.zerotier.sdk.PeerPhysicalPath;
import com.zerotier.sdk.ResultCode;
import com.zerotier.sdk.VirtualNetworkConfig;
import com.zerotier.sdk.VirtualNetworkConfigListener;
import com.zerotier.sdk.VirtualNetworkConfigOperation;
import com.zerotier.sdk.VirtualNetworkFrameListener;
import com.zerotier.sdk.VirtualNetworkStatus;

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.util.Arrays;

public class ZeroTierOneConnectionManager {
    private static final String TAG = "ZeroTierOne";
    private static ZeroTierOneConnectionManager instance;
    private Node node;
    private long currentNetworkId;
    private ZeroTierView zerotierView;
    private Activity activity;

    private ZeroTierOneConnectionManager() {
    }

    public static ZeroTierOneConnectionManager getInstance() {
        if (instance == null) {
            instance = new ZeroTierOneConnectionManager();
        }
        return instance;
    }

    public class ConnectionStatus {
        public static final String STATUS_CONNECTED = "连接成功";
        public static final String STATUS_DISCONNECTED = "连接失败";
        public static final String STATUS_WAITING_AUTH = "等待授权";

        public String status;
        public boolean isP2P;
        public int latency;
        public String assignedIp;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("状态：").append(status).append("\n");

            if (status.equals(STATUS_CONNECTED)) {
                sb.append("模式：").append(isP2P ? "P2P" : "中继").append("\n");
                sb.append("延迟：").append(latency).append("ms\n");
                sb.append("IP：").append(assignedIp != null ? assignedIp : "未分配");
            }
            return sb.toString();
        }
    }

    public void run(Activity activity, String networkIdHex) {
        this.activity = activity;

        Intent intent = VpnService.prepare(activity);
        if (intent != null) {
            activity.startActivityForResult(intent, 1);
            return;
        }

        initAndStart(networkIdHex);
    }
    private DatagramSocket udpSocket;
    private void initAndStart(String networkIdHex) {
        if (zerotierView == null) {
            zerotierView = new ZeroTierView(activity);
        }

        if (networkIdHex == null || networkIdHex.isEmpty()) {
            zerotierView.updateVisibility(false);
            return;
        }

        zerotierView.updateVisibility(true);
        try {
            // 修改 UDP Socket 配置
            udpSocket = new DatagramSocket(9993);  // 使用固定端口
            udpSocket.setReuseAddress(true);
            udpSocket.setBroadcast(true);
            udpSocket.setSoTimeout(500);
            Log.d(TAG, "UDP Socket created on port: " + udpSocket.getLocalPort());
        } catch (Exception e) {
            Log.e(TAG, "Failed to create UDP socket", e);
            zerotierView.toastInfo("Failed to create UDP socket");
            return;
        }
        Thread daemonThread = new Thread(() -> {
            try {
                if (node == null) {
                    node = new Node(System.currentTimeMillis());
                    ResultCode result = node.init(
                            new DataStoreGetListener() {
                                @Override
                                public long onDataStoreGet(String name, byte[] buf) {
                                    try {
                                        File file = new File(activity.getFilesDir(),  "zerotier/" + name);
                                        if (!file.exists()) {
                                            return 0;
                                        }
                                        byte[] data = null;
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            data = Files.readAllBytes(file.toPath());
                                        }
                                        System.arraycopy(data, 0, buf, 0, data.length);
                                        return data.length;
                                    } catch (Exception e) {
                                        Log.e(TAG, "读取存储失败: " + name, e);
                                        return 0;
                                    }
                                }
                            },
                            new DataStorePutListener() {
                                @Override
                                public int onDataStorePut(String name, byte[] data, boolean secure) {
                                    try {
                                        // 获取基础目录
                                        File baseDir = new File(activity.getFilesDir(), "zerotier");

                                        // 处理可能包含子目录的文件名
                                        File file = new File(baseDir, name);

                                        // 确保父目录存在
                                        File parentDir = file.getParentFile();
                                        if (parentDir != null) {
                                            parentDir.mkdirs();
                                        }

                                        // 写入文件
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            Files.write(file.toPath(), data);
                                        }
                                        Log.d(TAG, "成功写入存储: " + name);
                                        return 1;
                                    } catch (Exception e) {
                                        Log.e(TAG, "写入存储失败: " + name, e);
                                        return 0;
                                    }
                                }


                                @Override
                                public int onDelete(String name) {
                                    try {
                                        File file = new File(activity.getFilesDir(), "zerotier/"  + name);
                                        file.delete();
                                        return 1;
                                    } catch (Exception e) {
                                        Log.e(TAG, "删除存储失败: " + name, e);
                                        return 0;
                                    }
                                }
                            },
                            new PacketSender() {
                                @Override
                                public int onSendPacketRequested(long localSocket,
                                                                 InetSocketAddress remoteAddr,
                                                                 byte[] packetData,
                                                                 int ttl) {
                                    try {
                                        DatagramPacket packet = new DatagramPacket(
                                                packetData,
                                                packetData.length,
                                                remoteAddr.getAddress(),
                                                remoteAddr.getPort()
                                        );

                                        udpSocket.send(packet);
                                        Log.d(TAG, String.format("Sent packet to %s:%d, size: %d",
                                                remoteAddr.getAddress().getHostAddress(),
                                                remoteAddr.getPort(),
                                                packetData.length));
                                        return packetData.length;
                                    } catch (Exception e) {
                                        Log.e(TAG, "Failed to send packet to " + remoteAddr, e);
                                        return 0;
                                    }
                                }
                            },
                            new EventListener() {
                                @Override
                                public void onEvent(Event event) {
                                    Log.d(TAG, "ZeroTier事件: " + event.toString());
                                }
                                @Override
                                public void onTrace(String message) {
                                    Log.d(TAG, "ZeroTier日志: " + message);
                                }
                            },
                            new VirtualNetworkFrameListener() {
                                @Override
                                public void onVirtualNetworkFrame(long nwid, long srcMac,
                                                                  long destMac, long etherType, long vlanId,
                                                                  byte[] frameData) {
                                }
                            },
                            new VirtualNetworkConfigListener() {
                                @Override
                                public int onNetworkConfigurationUpdated(long nwid,
                                                                         VirtualNetworkConfigOperation op,
                                                                         VirtualNetworkConfig config) {
                                    return 0;
                                }
                            },
                            null); // pathChecker可选

                    if (result != ResultCode.RESULT_OK) {
                        throw new RuntimeException("Node初始化失败: " + result);
                    }
                }
                // 3. 启动接收线程
                Thread receiverThread = new Thread(() -> {
                    byte[] buffer = new byte[10000];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    long[] nextBackgroundTaskDeadline = new long[1];

                    while (!Thread.interrupted()) {
                        try {
                            udpSocket.receive(packet);

                            // 正确调用 processWirePacket
                            ResultCode result = node.processWirePacket(
                                    System.currentTimeMillis(),    // 当前时间
                                    udpSocket.getLocalPort(),      // 本地 socket
                                    new InetSocketAddress(         // 远程地址
                                            packet.getAddress(),
                                            packet.getPort()
                                    ),
                                    Arrays.copyOf(packet.getData(), packet.getLength()),  // 数据包内容
                                    nextBackgroundTaskDeadline     // 下一次后台任务的截止时间
                            );

                            // 添加后台任务处理
                            if (nextBackgroundTaskDeadline[0] > 0) {
                                node.processBackgroundTasks(System.currentTimeMillis(), nextBackgroundTaskDeadline);
                            }

                            // 重置 packet 以便下次接收
                            packet.setLength(buffer.length);
                        } catch (SocketTimeoutException ignored) {
                            // 超时时也处理后台任务
                            try {
                                node.processBackgroundTasks(System.currentTimeMillis(), nextBackgroundTaskDeadline);
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing background tasks", e);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error receiving packet", e);
                        }
                    }
                });
                receiverThread.setDaemon(true);
                receiverThread.start();
                Log.d(TAG, "Waiting for node to come online...");
                int attempts = 0;
                while (attempts < 30) { // 最多等待30秒
                    NodeStatus status = node.status();
                    if (status.isOnline()) {
                        Log.d(TAG, "Node is online! Address: " +
                                String.format("%010x", status.getAddress()));
                        break;
                    }
                    Log.d(TAG, "Node not online yet, waiting... Attempt: " + (attempts + 1));
                    Thread.sleep(1000); // 每秒检查一次
                    attempts++;
                }

                if (!node.status().isOnline()) {
                    throw new RuntimeException("Node failed to come online after 30 seconds");
                }
                currentNetworkId = Long.parseLong(networkIdHex, 16);
                ResultCode joinResult = node.join(currentNetworkId);
                if (joinResult != ResultCode.RESULT_OK) {
                    throw new RuntimeException("加入网络失败: " + joinResult);
                }

                startStatusMonitor();

            } catch (Exception e) {
                Log.e(TAG, "ZeroTier启动失败", e);
                zerotierView.toastInfo("ZeroTier启动失败");
            }
        });
        daemonThread.setDaemon(true);
        daemonThread.start();
    }

    private ConnectionStatus checkConnectionStatus() {
        ConnectionStatus status = new ConnectionStatus();

        try {
            if (node == null) {
                status.status = ConnectionStatus.STATUS_DISCONNECTED;
                return status;
            }

            NodeStatus nodeStatus = node.status();
            if (!nodeStatus.isOnline()) {
                status.status = ConnectionStatus.STATUS_DISCONNECTED;
                return status;
            }

            VirtualNetworkConfig networkConfig = node.networkConfig(currentNetworkId);
            if (networkConfig != null) {
                VirtualNetworkStatus  networkStatus=  networkConfig.getStatus();
                if (networkStatus == VirtualNetworkStatus.NETWORK_STATUS_REQUESTING_CONFIGURATION || networkStatus == VirtualNetworkStatus.NETWORK_STATUS_ACCESS_DENIED){
                    status.status = ConnectionStatus.STATUS_WAITING_AUTH;
                }else if (networkStatus == VirtualNetworkStatus.NETWORK_STATUS_OK){
                    status.status = ConnectionStatus.STATUS_CONNECTED;
                    // 获取IP
                    InetSocketAddress[] addresses = networkConfig.getAssignedAddresses();
                    if (addresses != null && addresses.length > 0) {
                        status.assignedIp = addresses[0].getAddress().getHostAddress();
                    }

                    // 检查P2P状态和延迟
                    Peer[] peers = node.peers();
                    if (peers != null) {
                        for (Peer peer : peers) {
                            int peerLatency = peer.getLatency();
                            if (peerLatency > 0) {
                                status.latency = peerLatency;
                                PeerPhysicalPath[] paths = peer.getPaths();
                                if (paths != null) {
                                    for (PeerPhysicalPath path : paths) {
                                        if (path.isPreferred()) {
                                            status.isP2P = true;
                                            break;
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }else {
                    status.status = ConnectionStatus.STATUS_DISCONNECTED;
                }
            } else {
                status.status = ConnectionStatus.STATUS_DISCONNECTED;
            }

        } catch (Exception e) {
            Log.e(TAG, "检查状态失败", e);
            status.status = ConnectionStatus.STATUS_DISCONNECTED;
        }

        return status;
    }

    private void startStatusMonitor() {
        new Thread(() -> {
            while (node != null) {
                try {
                    ConnectionStatus status = checkConnectionStatus();
                    zerotierView.updateStatus(status.toString());
                    Log.d(TAG,status.toString());
                    Thread.sleep(2000);
                } catch (Exception e) {
                    Log.e(TAG, "状态监控失败", e);
                }
            }
        }).start();
    }

}