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

/**
 * ZeroTier UI 管理类
 */
public class ZeroTierView {
    private final Activity activity;
    private final ImageButton button;
    private final Handler mainHandler;
    private boolean isEnabled = false;
    private String currentStatus = "";
    private AlertDialog dialog;

    public ZeroTierView(Activity activity) {
        this.activity = activity;
        this.button = activity.findViewById(R.id.zeroTierButton);
        this.mainHandler = new Handler(Looper.getMainLooper());
        initButton();
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
        builder.setMessage(currentStatus);
        builder.setPositiveButton("确定", (dialog, which) -> dialog.dismiss());

        dialog = builder.create();
        dialog.show();
    }

    /**
     * 更新按钮状态
     */
    public void updateButtonState(boolean enabled) {
        mainHandler.post(() -> {
            isEnabled = enabled;
            button.setAlpha(enabled ? 1.0f : 0.5f);
        });
    }

    /**
     * 更新状态信息
     */
    public void updateStatus(String status) {
        mainHandler.post(() -> {
            currentStatus = status;
            // 如果对话框正在显示，更新内容
            if (dialog != null && dialog.isShowing()) {
                dialog.setMessage(status);
            }
        });
    }

    public void toastInfo(String info){
        mainHandler.post(() -> {
            if (!isEnabled) {
                Toast.makeText(activity, info, Toast.LENGTH_SHORT).show();
            }
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
}

