package com.limelight.zerotier;

import android.content.Context;
import android.util.Log;
import com.limelight.binding.ZeroTier;
import java.io.File;

public class ZeroTierManager {
    private static final String TAG = "ZeroTierManager";
    private static final String NETWORK_ID = "52b337794f6ad61f"; // 替换为你的网络ID
    private final Context context;

    public ZeroTierManager(Context context) {
        this.context = context;
    }

    public void initialize() {
        try {
            File storageDir = new File(context.getFilesDir(), "zerotier");
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            
            Log.d(TAG, "Starting ZeroTier node with path: " + storageDir.getAbsolutePath());
            int result = ZeroTier.startNode(storageDir.getAbsolutePath());
            Log.i(TAG, "ZeroTier node started with result: " + result);
            
            if (result == 0) {
                joinNetwork();
            } else {
                Log.e(TAG, "Failed to start ZeroTier node, error code: " + result);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize ZeroTier", e);
        }
    }

    private void joinNetwork() {
        try {
            Log.d(TAG, "Attempting to join network: " + NETWORK_ID);
            int result = ZeroTier.joinNetwork(NETWORK_ID);
            Log.i(TAG, "Join network result: " + result);
            
            if (result != 0) {
                Log.e(TAG, "Failed to join network, error code: " + result);
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while joining network", e);
        }
    }

    public int getNetworkStatus() {
        try {
            int status = ZeroTier.getNetworkStatus(NETWORK_ID);
            Log.d(TAG, "Network status: " + status);
            return status;
        } catch (Exception e) {
            Log.e(TAG, "Error getting network status", e);
            return -1;
        }
    }

    public String[] getNetworkAddresses() {
        try {
            String[] addresses = ZeroTier.getNetworkAddresses(NETWORK_ID);
            Log.d(TAG, "Got addresses: " + (addresses != null ? addresses.length : 0));
            if (addresses != null) {
                for (String addr : addresses) {
                    Log.d(TAG, "Address: " + addr);
                }
            }
            return addresses;
        } catch (Exception e) {
            Log.e(TAG, "Error getting network addresses", e);
            return new String[0];
        }
    }
} 