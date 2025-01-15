package com.limelight.zerotier;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.zerotier.events.VirtualNetworkConfigChangedEvent;
import com.zerotier.sdk.VirtualNetworkConfig;
import com.zerotier.sdk.VirtualNetworkStatus;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * ZeroTier UI 管理类
 */
public class ZeroTierView {
    private final Activity activity;
    private final ImageButton button;
    private final Handler mainHandler;
    private AlertDialog dialog;
    private String msg = "";
    private final EventBus eventBus = EventBus.getDefault();
    // 添加闪烁动画相关变量
    private static final int BLINK_INTERVAL = 500; // 闪烁间隔(毫秒)
    private boolean isBlinking = false;
    private final Runnable blinkRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBlinking) {
                button.animate()
                        .alpha(button.getAlpha() > 0.7f ? 0.5f : 1.0f)
                        .setDuration(BLINK_INTERVAL)
                        .withEndAction(() -> {
                            if (isBlinking) {
                                mainHandler.postDelayed(blinkRunnable, 100); // 短暂延迟后开始下一次动画
                            }
                        });
            }
        }
    };
    public ZeroTierView(Activity activity) {
        this.activity = activity;
        this.button = activity.findViewById(R.id.zeroTierButton);
        this.mainHandler = new Handler(Looper.getMainLooper());
        initButton();
        this.eventBus.register(this);
    }

    private void initButton() {
        button.setAlpha(0.5f);  // 默认置灰
        button.setOnClickListener(v -> showStatusDialog());
    }

    private void showStatusDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("ZeroTier 状态");
        builder.setMessage(msg);
        builder.setPositiveButton("确定", (dialog, which) -> dialog.dismiss());
        dialog = builder.create();
        dialog.show();
    }

    /**
     * 更新按钮状态
     */
    public void updateButtonState(boolean enabled) {
        mainHandler.post(() -> {
            button.setAlpha(enabled ? 1.0f : 0.5f);
        });
    }

    /**
     * 更新状态信息
     */
    public void updateStatus(String msg) {
        mainHandler.post(() -> {
            this.msg = msg;
            // 如果对话框正在显示，更新内容
            if (dialog != null && dialog.isShowing()) {
                dialog.setMessage(msg);
            }
        });
    }

    public void toastInfo(String info){
        mainHandler.post(() -> {
            Toast.makeText(activity, info, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 更新按钮可见性
     */
    public void updateVisibility(boolean visible) {
        mainHandler.post(() -> {
            button.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            if (!visible && dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onVirtualNetworkConfigChanged(VirtualNetworkConfigChangedEvent event) {
        VirtualNetworkConfig config = event.getVirtualNetworkConfig();
        VirtualNetworkStatus status = config.getStatus();
        var networkId = com.zerotier.sdk.util.StringUtils.networkIdToString(config.getNwid());
        if (status == VirtualNetworkStatus.NETWORK_STATUS_OK){
            if (isBlinking) {
                stopBlink();
            }
            updateStatus("连接成功");
        }else if (status == VirtualNetworkStatus.NETWORK_STATUS_ACCESS_DENIED){
            startBlink();
            updateStatus("访问被拒绝，请检查权限");
        }else if (status == VirtualNetworkStatus.NETWORK_STATUS_NOT_FOUND){
            updateStatus("网络不存在");
        }else if (status == VirtualNetworkStatus.NETWORK_STATUS_AUTHENTICATION_REQUIRED){
            updateStatus("请授权网络");
        }
    else if (status == VirtualNetworkStatus.NETWORK_STATUS_REQUESTING_CONFIGURATION){

    }
}

/**
 * 开始按钮闪烁
 */
public void startBlink() {
    mainHandler.post(() -> {
        if (!isBlinking) {
            isBlinking = true;
            blinkRunnable.run();
        }
    });
}

/**
 * 停止按钮闪烁
 */
public void stopBlink() {
    mainHandler.post(() -> {
        isBlinking = false;
        mainHandler.removeCallbacks(blinkRunnable);
        // 恢复按钮状态
        button.setAlpha(1.0f);
    });
}

@Override
protected void finalize() throws Throwable {
    super.finalize();
    // 确保清理 Handler 回调
    mainHandler.removeCallbacks(blinkRunnable);
}

}

