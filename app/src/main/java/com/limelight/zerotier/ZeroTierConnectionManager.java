package com.limelight.zerotier;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.zerotier.sockets.ZeroTierEventListener;
import com.zerotier.sockets.ZeroTierNative;
import com.zerotier.sockets.ZeroTierNode;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * ZeroTier连接管理器
 * 提供连接、断开、自动重连等功能
 */
public class ZeroTierConnectionManager {
    // 单例
    private static ZeroTierConnectionManager instance;
    private  String info = "";
    private  boolean connected = false;
    private  long currentNetworkId;
    private  ZeroTierNode node;
    private  String storagePath;
    private  ZeroTierView zerotierView;

    private ZeroTierConnectionManager() {
    }

    public static ZeroTierConnectionManager getInstance() {
        if (instance == null) {
            instance = new ZeroTierConnectionManager();
        }
        return instance;
    }
    /**
     * 启动ZeroTier
     *
     * @param activity      上下文
     * @param networkIdHex 16进制的网络ID
     */
    public void run(Activity activity, String networkIdHex) {
        // 初始化 UI 组件
        if (zerotierView == null) {
            zerotierView = new ZeroTierView(activity);
        }

        if (networkIdHex == null || networkIdHex.isEmpty()) {
            zerotierView.updateVisibility(false);
            return;
        }

        // 显示按钮
        zerotierView.updateVisibility(true);


        new Thread(() -> {
            if (node == null) {
                storagePath = activity.getFilesDir().getPath() + "/zerotier";
                try {
                    node = new ZeroTierNode();
                    node.initFromStorage(storagePath);
                    node.initSetEventHandler(new ConnectionEventListener());
                    node.initAllowPeerCache(true);
                    node.initAllowIdCache(true);
                    node.initAllowRootsCache(true);
                    node.initAllowNetworkCache(true);
                    node.initSetPort((short)9994);
                    node.start();

                } catch (ExceptionInInitializerError e) {
                    zerotierView.updateStatus("ZeroTier 初始化失败");
                    return;
                }
            }

            try {
                long networkId = new BigInteger(networkIdHex, 16).longValue();
                if (networkId == currentNetworkId) {
                    return;
                }
//                node.stop();
                currentNetworkId = networkId;
                while (!node.isOnline()) {
                    zerotierView.updateStatus("等待节点上线");
                    Thread.sleep(300);
                }
                node.join(currentNetworkId);
                System.out.println();
            } catch (Exception e) {
                zerotierView.toastInfo("networkId错误");

            }
        }).start();
    }

    private class ConnectionEventListener implements ZeroTierEventListener {
        private static final String TAG = "ZeroTier";

        @Override
        public void onZeroTierEvent(long id, int eventCode) {
            // 添加事件日志
            Log.d(TAG, String.format("Event: %d, Network: %d", eventCode, id));

            if (id != currentNetworkId && eventCode != ZeroTierNative.ZTS_EVENT_NODE_OFFLINE) {
                return;
            }
            if (id == ZeroTierNative.ZTS_EVENT_PEER_RELAY) {
                Log.d(TAG, "切换到中继模式");
                node.join(currentNetworkId);
            }
            if (eventCode == ZeroTierNative.ZTS_EVENT_NODE_OFFLINE) {
                // 节点离线后立即重新启动并加入网络
                connected = false;
                info = "设备网络已断开，正在重新连接...";
                try {
                    Thread.sleep(3000);
                    node.start();
                    while (!node.isOnline()) {
                        zerotierView.updateStatus("等待节点上线");
                        Thread.sleep(1000);
                    }
                    node.join(currentNetworkId);
                } catch (Exception e) {
                    info = "重新连接失败";
                }
            } else if (eventCode == ZeroTierNative.ZTS_EVENT_NODE_ONLINE) {
                // 节点上线，正在连接网络
                info = "正在连接到虚拟网络...";

            } else if (eventCode == ZeroTierNative.ZTS_EVENT_NETWORK_REQ_CONFIG) {
                // 正在请求网络配置
                info = "正在验证网络权限...";
//                ZeroTierNative .zts_set_recv_timeout()
            } else if (eventCode == ZeroTierNative.ZTS_EVENT_NETWORK_ACCESS_DENIED) {
                // 网络访问被拒绝
                connected = false;
                info = "无访问权限：请在ZeroTier管理页面授权此设备";

            } else if (eventCode == ZeroTierNative.ZTS_EVENT_NETWORK_OK) {
                // 网络配置已接受
                Log.d(TAG, "Network configuration accepted");
                info = "网络验证通过，正在配置...";

            } else if (eventCode == ZeroTierNative.ZTS_EVENT_NETWORK_READY_IP4) {
                // 获得IP地址，可以开始通信
                Log.d(TAG, "Network IPv4 ready");
                connected = true;
                try {
                    String ip = node.getIPv4Address(currentNetworkId).getHostAddress();
                    info = "连接成功，IP地址: " + ip;
                    Log.d(TAG, "IP address: " + ip);
                    int pathCount = ZeroTierNative.zts_core_query_path_count(currentNetworkId);
                    if (pathCount > 0) {
                        // 遍历并尝试所有可能的路径
                        for (int i = 0; i < pathCount; i++) {
                            String path = new String(new char[ZeroTierNative.ZTS_IP_MAX_STR_LEN]);
                            ZeroTierNative.zts_core_query_path(currentNetworkId, i, path,
                                    ZeroTierNative.ZTS_IP_MAX_STR_LEN);
                            Log.d(TAG, "Pathdfg: " + path);
                        }
                    }
                } catch (Exception e) {
                    info = "连接成功";
                    Log.e(TAG, "Failed to get IP address", e);
                }

            } else if (eventCode == ZeroTierNative.ZTS_EVENT_NETWORK_DOWN) {
                // 网络断开
                connected = false;
                info = "与虚拟网络断开连接";

            } else if (eventCode == ZeroTierNative.ZTS_EVENT_NETWORK_NOT_FOUND) {
                // 网络未找到
                connected = false;
                info = "网络不存在，请检查网络ID是否正确";
            } else if (eventCode == ZeroTierNative.ZTS_EVENT_NETWORK_UPDATE) {
                Log.d(TAG, "Network configuration updated");
                // 可能需要处理配置更新
            }

            // 状态变化时通知
            notifyStateChanged();
        }
    }

    /**
     * 获取当前网络中的所有成员IP
     *
     * @return 成员IP列表
     */
//    public List<String> getNetworkMembers() {
//        List<String> members = new ArrayList<>();
//        if (!connected || node == null) {
//            return members;
//        }
//
//        try {
//            // 获取当前网络中的成员数量
//            int peerCount = ZeroTierNative.zts_core_query_peer_count();
//
//            for (int i = 0; i < peerCount; i++) {
//                // 获取每个成员的IP地址
//                String addr = "";
//                int result = ZeroTierNative.zts_core_query_addr(currentNetworkId, i, addr, addr.length());
//                if (result == 0 && !addr.isEmpty()) {
//                    members.add(addr);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return members;
//    }

    private void notifyStateChanged() {
        if (zerotierView != null) {
            zerotierView.updateButtonState(connected);
            zerotierView.updateStatus(info);
        }
    }
}
