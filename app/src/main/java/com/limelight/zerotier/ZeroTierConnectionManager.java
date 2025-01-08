//package com.limelight.zerotier;
//
//import android.app.Activity;
//import android.util.Log;
//import android.widget.Toast;
//
//import com.zerotier.sockets.ZeroTierEventListener;
//import com.zerotier.sockets.ZeroTierNative;
//import com.zerotier.sockets.ZeroTierNode;
//
//import java.math.BigInteger;
//import java.net.InetAddress;
//import java.util.ArrayList;
//import java.util.List;
//import java.io.File;
//
///**
// * ZeroTier连接管理器
// * 提供连接、断开、自动重连等功能
// */
//public class ZeroTierConnectionManager {
//    // 单例
//    private static ZeroTierConnectionManager instance;
//    private String info = "";
//    private boolean connected = false;
//    private long currentNetworkId;
//    private ZeroTierNode node;
//    private String storagePath;
//    private ZeroTierView zerotierView;
//
//    private ZeroTierConnectionManager() {
//    }
//
//    public static ZeroTierConnectionManager getInstance() {
//        if (instance == null) {
//            instance = new ZeroTierConnectionManager();
//        }
//        return instance;
//    }
//
//    /**
//     * 启动ZeroTier
//     *
//     * @param activity      上下文
//     * @param networkIdHex 16进制的网络ID
//     */
//    public void run(Activity activity, String networkIdHex) {
//        if (zerotierView == null) {
//            zerotierView = new ZeroTierView(activity);
//        }
//
//        if (networkIdHex == null || networkIdHex.isEmpty()) {
//            zerotierView.updateVisibility(false);
//            return;
//        }
//
//        zerotierView.updateVisibility(true);
//
//        Thread daemonThread = new Thread(() -> {
//            try {
//                if (node == null) {
//                    storagePath = activity.getFilesDir().getPath() + "/zerotier";
//                    // 检查是否已经存在节点配置
//                    if (!new File(storagePath).exists()) {
//                        // 如果不存在，创建目录
//                        new File(storagePath).mkdirs();
//                    }
//
//                    node = new ZeroTierNode();
//                    node.initFromStorage(storagePath);
//                    node.initSetEventHandler(new ConnectionEventListener());
//                    node.initAllowPeerCache(true);
//                    node.initAllowIdCache(false);
//                    node.initAllowRootsCache(false);
//                    node.initAllowNetworkCache(false);
//                    node.initSetPort((short)9993);
//                    node.start();
//                }
//
//                // 加入网络
//                long networkId = new BigInteger(networkIdHex, 16).longValue();
//                if (networkId != currentNetworkId) {
//                    currentNetworkId = networkId;
//                    while (!node.isOnline()) {
//                        zerotierView.updateStatus("等待节点上线");
//                        Thread.sleep(300);
//                    }
//                    node.join(currentNetworkId);
//                }
//            } catch (Exception e) {
//                zerotierView.toastInfo("ZeroTier 初始化或连接失败");
//            }
//        });
//        daemonThread.setDaemon(true);
//        daemonThread.start();
//    }
//
//    private class ConnectionEventListener implements ZeroTierEventListener {
//        private static final String TAG = "ZeroTier";
//
//        @Override
//        public void onZeroTierEvent(long id, int eventCode) {
//            // 添加事件日志
//            String ip = null;
//            try {
//                 ip = node.getIPv4Address(currentNetworkId).getHostAddress();
//            } catch (Exception e) {
//                info = "连接成功";
//            }
//            Log.d(TAG, String.format("Event: %d, Network: %d, ip: %s", eventCode, id, ip));
//
//        }
//    }
//
//    /**
//     * 获取当前网络中的所有成员IP
//     *
//     * @return 成员IP列表
//     */
////    public List<String> getNetworkMembers() {
////        List<String> members = new ArrayList<>();
////        if (!connected || node == null) {
////            return members;
////        }
////
////        try {
////            // 获取当前网络中的成员数量
////            int peerCount = ZeroTierNative.zts_core_query_peer_count();
////
////            for (int i = 0; i < peerCount; i++) {
////                // 获取每个成员的IP地址
////                String addr = "";
////                int result = ZeroTierNative.zts_core_query_addr(currentNetworkId, i, addr, addr.length());
////                if (result == 0 && !addr.isEmpty()) {
////                    members.add(addr);
////                }
////            }
////        } catch (Exception e) {
////            e.printStackTrace();
////        }
////        return members;
////    }
//
//    private void notifyStateChanged() {
//        if (zerotierView != null) {
//            zerotierView.updateButtonState(connected);
//            zerotierView.updateStatus(info);
//        }
//    }
//}
