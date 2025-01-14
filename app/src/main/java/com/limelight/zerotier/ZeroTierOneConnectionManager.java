package com.limelight.zerotier;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.VpnService;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.limelight.zerotier.db.ZTDatabase;
import com.limelight.zerotier.model.Network;
import com.limelight.zerotier.model.NetworkConfig;
import com.limelight.zerotier.model.type.NetworkStatus;
import com.limelight.zerotier.model.type.NetworkType;
import com.limelight.zerotier.service.ZeroTierOneService;
import com.limelight.zerotier.util.Constants;
import com.limelight.zerotier.util.NetworkInfoUtils;

public class ZeroTierOneConnectionManager {
    private static final String TAG = "ZeroTierOne";
    private static final int VPN_REQUEST_CODE = 24; // 定义请求码
    private static ZeroTierOneConnectionManager instance;
    private ZeroTierView zerotierView;
    private Activity activity;
    private ZeroTierOneService boundService;
    private boolean isBound = false;
    private Long pendingNetworkId;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            boundService = ((ZeroTierOneService.ZeroTierBinder) service).getService();
            setIsBound(true);
            boundService.joinNetwork(pendingNetworkId);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            boundService = null;
            setIsBound(false);
        }
    };

    private ZeroTierOneConnectionManager() {
    }

    public static ZeroTierOneConnectionManager getInstance() {
        if (instance == null) {
            instance = new ZeroTierOneConnectionManager();
        }
        return instance;
    }

    private synchronized void setIsBound(boolean bound) {
        this.isBound = bound;
    }

    public synchronized boolean isBound() {
        return isBound;
    }

    /**
     * 绑定服务
     */
    private void doBindService() {
        if (!isBound() && activity != null) {
            Intent intent = new Intent(activity, ZeroTierOneService.class);
            if (activity.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
                setIsBound(true);
            }
        }
    }

    /**
     * 解绑服务
     */
    private void doUnbindService() {
        if (isBound() && activity != null) {
            try {
                activity.unbindService(connection);
            } catch (Exception e) {
                Log.e(TAG, "Error unbinding service", e);
            }
            setIsBound(false);
        }
    }

    /**
     * 启动 ZeroTier 服务并连接到指定网络
     */
    public void run(Activity activity, String networkIdHex) {
        this.activity = activity;

long networkId;
        try {
            networkId = Long.parseLong(networkIdHex, 16);
            // 初始化网络数据
            initNetworkData(networkId, networkIdHex);
        } catch (NumberFormatException e) {
            Log.e(TAG, "init network error", e);
            Toast.makeText(activity, "网络ID格式错误", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查网络环境
        var preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean useCellularData = preferences.getBoolean(Constants.PREF_NETWORK_USE_CELLULAR_DATA, false);
        var currentNetworkInfo = NetworkInfoUtils.getNetworkInfoCurrentConnection(activity);

        if (currentNetworkInfo == NetworkInfoUtils.CurrentConnection.CONNECTION_NONE) {
            // 未连接网络
            Toast.makeText(activity, "未连接网络", Toast.LENGTH_SHORT).show();
            return;
        }

        // 保存待连接的网络ID
        this.pendingNetworkId = networkId;

        // 检查 VPN 权限
        Intent prepare = VpnService.prepare(activity);
        if (prepare != null) {
            // 需要请求 VPN 权限
            activity.startActivityForResult(prepare, VPN_REQUEST_CODE);
            return;
        }

        // 已有VPN权限,直接启动服务
        startZeroTierService(networkId);
    }

    /**
     * 处理VPN权限请求结果
     */
    public void onActivityResult(int requestCode, int resultCode) {
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && pendingNetworkId != null) {
                // 获得VPN权限,启动服务
                startZeroTierService(pendingNetworkId);
            } else {
                Toast.makeText(activity, "VPN权限请求被拒绝", Toast.LENGTH_SHORT).show();
                pendingNetworkId = null;
            }
        }
    }

    /**
     * 启动ZeroTier服务
     */
    private void startZeroTierService(long networkId) {
        Intent intent = new Intent(activity, ZeroTierOneService.class);
        intent.putExtra(ZeroTierOneService.ZT1_NETWORK_ID, networkId);

        // 绑定服务
        doBindService();

        // 启动服务
        activity.startService(intent);

        // joinNetwork会在onServiceConnected中调用
    }

    /**
     * 停止 ZeroTier 服务
     */
    public void stop() {
        if (boundService != null) {
            boundService.stopZeroTier();
        }

        if (activity != null) {
            Intent intent = new Intent(activity, ZeroTierOneService.class);
            activity.stopService(intent);
        }

        doUnbindService();
    }

    /**
     * 离开指定网络
     */
    public void leaveNetwork(long networkId) {
        if (boundService != null) {
            boundService.leaveNetwork(networkId);
        }
    }

    /**
     * 加入指定网络
     */
    public void joinNetwork(long networkId) {
        if (boundService != null) {
            boundService.joinNetwork(networkId);
        }
    }

    private void initNetworkData(long networkId, String networkIdHex) {
        ZTDatabase db = ZTDatabase.getInstance(activity);

        // 检查网络是否已存在
        Network existNetwork = db.getNetworkById(networkId);
        if (existNetwork != null) {
            return;
        }

        // 创建新网络配置
        NetworkConfig config = new NetworkConfig();
        config.setId(networkId);
        config.setRouteViaZeroTier(true);
        config.setDnsMode(0);
        config.setType(NetworkType.PRIVATE);
        config.setStatus(NetworkStatus.REQUESTING_CONFIGURATION);
        db.saveNetworkConfig(config);

        // 创建网络
        Network network = new Network();
        network.setNetworkId(networkId);
        network.setNetworkIdStr(networkIdHex);
        network.setNetworkConfigId(networkId);
        network.setLastActivated(true);
        db.saveNetwork(network);
    }
}
