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

import com.limelight.zerotier.service.ZeroTierOneService;
import com.limelight.zerotier.util.Constants;
import com.limelight.zerotier.util.NetworkInfoUtils;

public class ZeroTierOneConnectionManager {
    private static final String TAG = "ZeroTierOne";
    private static ZeroTierOneConnectionManager instance;
    private ZeroTierView zerotierView;
    private Activity activity;
    private ZeroTierOneService boundService;
    private boolean isBound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            boundService = ((ZeroTierOneService.ZeroTierBinder) service).getService();
            setIsBound(true);
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
        
        // 转换网络ID
        long networkId;
        try {
            networkId = Long.parseLong(networkIdHex, 16);
        } catch (NumberFormatException e) {
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

        // 检查 VPN 权限
        Intent prepare = VpnService.prepare(activity);
        if (prepare != null) {
            // 需要请求 VPN 权限
            activity.startActivityForResult(prepare, 0);
            return;
        }

        // 启动服务
        Intent intent = new Intent(activity, ZeroTierOneService.class);
        intent.putExtra(ZeroTierOneService.ZT1_NETWORK_ID, networkId);
        
        // 绑定服务
        doBindService();
        
        // 启动服务
        activity.startService(intent);

        joinNetwork(networkId);
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
}