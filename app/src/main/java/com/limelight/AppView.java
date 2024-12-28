package com.limelight;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import com.limelight.computers.ComputerManagerListener;
import com.limelight.computers.ComputerManagerService;
import com.limelight.grid.AppGridAdapter;
import com.limelight.nvstream.http.ComputerDetails;
import com.limelight.nvstream.http.NvApp;
import com.limelight.nvstream.http.NvHTTP;
import com.limelight.nvstream.http.PairingManager;
import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.ui.AdapterFragment;
import com.limelight.ui.AdapterFragmentCallbacks;
import com.limelight.utils.CacheHelper;
import com.limelight.utils.Dialog;
import com.limelight.utils.ServerHelper;
import com.limelight.utils.ShortcutHelper;
import com.limelight.utils.SpinnerDialog;
import com.limelight.utils.UiHelper;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.app.AlertDialog;

import org.xmlpull.v1.XmlPullParserException;

public class AppView extends Activity implements AdapterFragmentCallbacks {
    private AppGridAdapter appGridAdapter;
    private String uuidString;
    private ShortcutHelper shortcutHelper;

    private ComputerDetails computer;
    private ComputerManagerService.ApplistPoller poller;
    private SpinnerDialog blockingLoadSpinner;
    private String lastRawApplist;
    private int lastRunningAppId;
    private boolean suspendGridUpdates;
    private boolean inForeground;
    private boolean showHiddenApps;
    private HashSet<Integer> hiddenAppIds = new HashSet<>();

    private PreferenceConfiguration prefConfig;

    private final static int START_OR_RESUME_ID = 1;
    private final static int QUIT_ID = 2;
    private final static int START_WITH_QUIT = 4;
    private final static int VIEW_DETAILS_ID = 5;
    private final static int CREATE_SHORTCUT_ID = 6;
    private final static int HIDE_APP_ID = 7;
    private final static int START_WITH_VDISPLAY = 20;
    private final static int START_WITH_QUIT_VDISPLAY = 21;

    public final static String HIDDEN_APPS_PREF_FILENAME = "HiddenApps";

    public final static String NAME_EXTRA = "Name";
    public final static String UUID_EXTRA = "UUID";
    public final static String NEW_PAIR_EXTRA = "NewPair";
    public final static String SHOW_HIDDEN_APPS_EXTRA = "ShowHiddenApps";

    private ComputerManagerService.ComputerManagerBinder managerBinder;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            final ComputerManagerService.ComputerManagerBinder localBinder =
                    (ComputerManagerService.ComputerManagerBinder) binder;

            // 为避免阻塞主线程，用子线程等待Binder完成初始化
            new Thread() {
                @Override
                public void run() {
                    localBinder.waitForReady();

                    // 获取 ComputerDetails
                    computer = localBinder.getComputer(uuidString);
                    if (computer == null) {
                        finish();
                        return;
                    }

                    // 初始化 AppGridAdapter
                    try {
                        appGridAdapter = new AppGridAdapter(
                                AppView.this,
                                PreferenceConfiguration.readPreferences(AppView.this),
                                computer, 
                                localBinder.getUniqueId(),
                                showHiddenApps
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                        finish();
                        return;
                    }

                    // 同步已隐藏应用
                    appGridAdapter.updateHiddenApps(hiddenAppIds, true);

                    // 令 managerBinder 可见
                    managerBinder = localBinder;

                    // 尝试从缓存加载应用列表，若失败则从网络加载
                    populateAppGridWithCache();

                    // 启动轮询，用于实时刷新应用列表
                    startComputerUpdates();

                    // 切回主线程，替换或装载列表 Fragment
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isChangingConfigurations()) {
                            try {
                                getFragmentManager().beginTransaction()
                                        .replace(R.id.appFragmentContainer, new AdapterFragment())
                                        .commitAllowingStateLoss();
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }.start();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            managerBinder = null;
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        this.prefConfig = PreferenceConfiguration.readPreferences(this);
        setContentView(R.layout.activity_app_view);
        UiHelper.notifyNewRootView(this);

        TextView label = findViewById(R.id.appListText);
        label.setText(getTitle());

        // If appGridAdapter is initialized, let it know about the configuration change.
        // If not, it will pick it up when it initializes.
        if (appGridAdapter != null) {
            // Update the app grid adapter to create grid items with the correct layout
            appGridAdapter.updateLayoutWithPreferences(this, this.prefConfig);

            try {
                // Reinflate the app grid itself to pick up the layout change
                getFragmentManager().beginTransaction()
                        .replace(R.id.appFragmentContainer, new AdapterFragment())
                        .commitAllowingStateLoss();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    private void startComputerUpdates() {
        if (managerBinder == null || !inForeground) {
            return;
        }

        managerBinder.startPolling(new ComputerManagerListener() {
            @Override
            public void notifyComputerUpdated(final ComputerDetails details) {
                if (suspendGridUpdates) {
                    return;
                }
                if (!details.uuid.equalsIgnoreCase(uuidString)) {
                    return;
                }

                // 如果 PC 已离线或已取消配对等，按原逻辑处理 ...
                // ...

                // 若服务端返回了新的应用列表 XML
                if (details.rawAppList != null && !details.rawAppList.equals(lastRawApplist)) {
                    lastRunningAppId = details.runningGameId;
                    lastRawApplist = details.rawAppList;
                    try {
                        List<NvApp> applist = NvHTTP.getAppListByReader(new StringReader(details.rawAppList));
                        updateUiWithAppList(applist);      // 调用你保留的更新方法
                        updateUiWithServerinfo(details);    // 若需要更新当前运行的 App 状态，也可调用
                        if (blockingLoadSpinner != null) {
                            blockingLoadSpinner.dismiss();
                            blockingLoadSpinner = null;
                        }
                    } catch (XmlPullParserException | IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    // 未更改应用列表，但 runningGameId 改变，也要刷新显示
                    if (details.runningGameId != lastRunningAppId) {
                        lastRunningAppId = details.runningGameId;
                        updateUiWithServerinfo(details);
                    }
                }
            }
        });

        // 若使用 poller, 别忘了创建并启动
        if (poller == null) {
            poller = managerBinder.createAppListPoller(computer);
        }
        poller.start();
    }

    private void stopComputerUpdates() {
        if (poller != null) {
            poller.stop();
        }

        if (managerBinder != null) {
            managerBinder.stopPolling();
        }

        if (appGridAdapter != null) {
            appGridAdapter.cancelQueuedOperations();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Assume we're in the foreground when created to avoid a race
        // between binding to CMS and onResume()
        inForeground = true;

        shortcutHelper = new ShortcutHelper(this);

        UiHelper.setLocale(this);

        setContentView(R.layout.activity_app_view);

        // Allow floating expanded PiP overlays while browsing apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setShouldDockBigOverlays(false);
        }

        UiHelper.notifyNewRootView(this);

        showHiddenApps = getIntent().getBooleanExtra(SHOW_HIDDEN_APPS_EXTRA, false);
        uuidString = getIntent().getStringExtra(UUID_EXTRA);

        SharedPreferences hiddenAppsPrefs = getSharedPreferences(HIDDEN_APPS_PREF_FILENAME, MODE_PRIVATE);
        for (String hiddenAppIdStr : hiddenAppsPrefs.getStringSet(uuidString, new HashSet<String>())) {
            hiddenAppIds.add(Integer.parseInt(hiddenAppIdStr));
        }

        String computerName = getIntent().getStringExtra(NAME_EXTRA);

        TextView label = findViewById(R.id.appListText);
        setTitle(computerName);
        label.setText(computerName);

        this.prefConfig = PreferenceConfiguration.readPreferences(this);

        // Bind to the computer manager service
        bindService(new Intent(this, ComputerManagerService.class), serviceConnection,
                Service.BIND_AUTO_CREATE);
    }

    private void updateHiddenApps(boolean hideImmediately) {
        HashSet<String> hiddenAppIdStringSet = new HashSet<>();

        for (Integer hiddenAppId : hiddenAppIds) {
            hiddenAppIdStringSet.add(hiddenAppId.toString());
        }

        getSharedPreferences(HIDDEN_APPS_PREF_FILENAME, MODE_PRIVATE)
                .edit()
                .putStringSet(uuidString, hiddenAppIdStringSet)
                .apply();

        appGridAdapter.updateHiddenApps(hiddenAppIds, hideImmediately);
    }

    private void populateAppGridWithCache() {
        try {
            lastRawApplist = CacheHelper.readInputStreamToString(
                    CacheHelper.openCacheFileForInput(getCacheDir(), "applist", uuidString));
            List<NvApp> applist = NvHTTP.getAppListByReader(new StringReader(lastRawApplist));
            updateUiWithAppList(applist);
        } catch (IOException | XmlPullParserException e) {
            if (lastRawApplist != null) {
                LimeLog.warning("Saved applist corrupted: " + lastRawApplist);
                e.printStackTrace();
            }
            LimeLog.info("Loading applist from the network");
            loadAppsBlocking(); // 后续得到响应时，会触发 notifyComputerUpdated
        }
    }

    private void loadAppsBlocking() {
        blockingLoadSpinner = SpinnerDialog.displayDialog(this, getResources().getString(R.string.applist_refresh_title),
                getResources().getString(R.string.applist_refresh_msg), true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SpinnerDialog.closeDialogs(this);
        Dialog.closeDialogs();

        if (managerBinder != null) {
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Display a decoder crash notification if we've returned after a crash
        UiHelper.showDecoderCrashDialog(this);

        inForeground = true;
        startComputerUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();

        inForeground = false;
        stopComputerUpdates();
    }

    @Override
    public void receiveAbsListView(AbsListView listView) {
        listView.setAdapter(appGridAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                AppObject app = (AppObject) appGridAdapter.getItem(pos);

                // 只要有正在运行的游戏就点开二级菜单，否则直接启动
                if (lastRunningAppId != 0 && lastRunningAppId == app.app.getAppId()) {
                    ServerHelper.doStart(AppView.this, app.app, computer, managerBinder, false);
                } else if (lastRunningAppId != 0) {
                    // 将原先的 openContextMenu(arg1) 换成显示对话框
                    showAppContextDialog(app, arg1);
                } else {
                    if (prefConfig.useVirtualDisplay && !(computer.vDisplaySupported && computer.vDisplayDriverReady)) {
                        UiHelper.displayVdisplayConfirmationDialog(
                                AppView.this,
                                computer,
                                () -> ServerHelper.doStart(AppView.this, app.app, computer, managerBinder, true),
                                null
                        );
                    } else {
                        ServerHelper.doStart(AppView.this, app.app, computer, managerBinder, prefConfig.useVirtualDisplay);
                    }
                }
            }
        });

        // 新增长按事件，弹出对话框
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            AppObject app = (AppObject) appGridAdapter.getItem(position);
            showAppContextDialog(app, view);
            return true;
        });

        UiHelper.applyStatusBarPadding(listView);
        listView.requestFocus();
    }

    private void showAppContextDialog(final AppObject app, final View targetView) {

        // 根据运行的逻辑组装菜单选项
        final ArrayList<String> menuItems = new ArrayList<>();

        // lastRunningAppId == 0 时的菜单选项
        if (lastRunningAppId == 0) {
            if (prefConfig.useVirtualDisplay) {
                menuItems.add(getResources().getString(R.string.applist_menu_start_primarydisplay)); // START_OR_RESUME_ID
            } else {
                menuItems.add(getResources().getString(R.string.applist_menu_start_vdisplay)); // START_WITH_VDISPLAY
            }
        } else {
            // 已有游戏在运行的菜单选项
            if (lastRunningAppId == app.app.getAppId()) {
                menuItems.add(getResources().getString(R.string.applist_menu_resume)); // START_OR_RESUME_ID
                menuItems.add(getResources().getString(R.string.applist_menu_quit));   // QUIT_ID
            } else {
                if (prefConfig.useVirtualDisplay) {
                    menuItems.add(getResources().getString(R.string.applist_menu_quit_and_start_vdisplay)); // START_WITH_QUIT_VDISPLAY
                    menuItems.add(getResources().getString(R.string.applist_menu_quit_and_start_primarydisplay)); // START_WITH_QUIT
                } else {
                    menuItems.add(getResources().getString(R.string.applist_menu_quit_and_start));         // START_WITH_QUIT
                    menuItems.add(getResources().getString(R.string.applist_menu_quit_and_start_vdisplay)); // START_WITH_QUIT_VDISPLAY
                }
            }
        }

        // 添加隐藏或显示选项
        if (lastRunningAppId != app.app.getAppId() || app.isHidden) {
            // HIDE_APP_ID
            // 这里不区分选中或未选中，通过点击后自行切换
            menuItems.add(getResources().getString(R.string.applist_menu_hide_app));
        }

        // 详细信息
        menuItems.add(getResources().getString(R.string.applist_menu_details));

        // 创建桌面快捷方式（只在 Android O+ 并且图片加载成功时显示）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ImageView appImageView = targetView.findViewById(R.id.grid_image);
            if (appImageView != null) {
                BitmapDrawable drawable = (BitmapDrawable) appImageView.getDrawable();
                if (drawable != null && drawable.getBitmap() != null) {
                    menuItems.add(getResources().getString(R.string.applist_menu_scut)); // CREATE_SHORTCUT_ID
                }
            }
        }

        // 使用 AlertDialog 显示菜单
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(menuItems.toArray(new String[0]), (dialog, which) -> {


            String selectedItem = menuItems.get(which);
            if (selectedItem.equals(getResources().getString(R.string.applist_menu_quit_and_start)) ||
                selectedItem.equals(getResources().getString(R.string.applist_menu_quit_and_start_vdisplay))) {
                boolean withVDiaplay = selectedItem.equals(getResources().getString(R.string.applist_menu_quit_and_start_vdisplay));
                if (withVDiaplay && !(computer.vDisplaySupported && computer.vDisplayDriverReady)) {
                    UiHelper.displayVdisplayConfirmationDialog(
                        AppView.this,
                        computer,
                        () -> ServerHelper.doStart(AppView.this, app.app, computer, managerBinder, true),
                        null
                    );
                } else {
                    ServerHelper.doStart(AppView.this, app.app, computer, managerBinder, withVDiaplay);
                }
            }
            else if (selectedItem.equals(getResources().getString(R.string.applist_menu_start_primarydisplay)) ||
                     selectedItem.equals(getResources().getString(R.string.applist_menu_start_vdisplay)) ||
                     selectedItem.equals(getResources().getString(R.string.applist_menu_resume))) {
                boolean withVDiaplay = selectedItem.equals(getResources().getString(R.string.applist_menu_start_vdisplay));
                if (withVDiaplay && !(computer.vDisplaySupported && computer.vDisplayDriverReady)) {
                    UiHelper.displayVdisplayConfirmationDialog(
                        AppView.this,
                        computer,
                        () -> ServerHelper.doStart(AppView.this, app.app, computer, managerBinder, true),
                        null
                    );
                } else {
                    ServerHelper.doStart(AppView.this, app.app, computer, managerBinder, withVDiaplay);
                }
            }
            else if (selectedItem.equals(getResources().getString(R.string.applist_menu_quit))) {
                suspendGridUpdates = true;
                ServerHelper.doQuit(AppView.this, computer, app.app, managerBinder, () -> {
                    suspendGridUpdates = false;
                    if (poller != null) {
                        poller.pollNow();
                    }
                });
            }
            else if (selectedItem.equals(getResources().getString(R.string.applist_menu_details))) {
                Dialog.displayDialog(AppView.this, getResources().getString(R.string.title_details), app.app.toString(), false);
            }
            else if (selectedItem.equals(getResources().getString(R.string.applist_menu_hide_app))) {
                if (app.isHidden) {
                    hiddenAppIds.remove(app.app.getAppId());
                } else {
                    hiddenAppIds.add(app.app.getAppId());
                }
                updateHiddenApps(false);
            }
            else if (selectedItem.equals(getResources().getString(R.string.applist_menu_scut))) {
                ImageView appImageView = targetView.findViewById(R.id.grid_image);
                Bitmap appBits = ((BitmapDrawable) appImageView.getDrawable()).getBitmap();
                if (!shortcutHelper.createPinnedGameShortcut(computer, app.app, appBits)) {
                    Toast.makeText(AppView.this, getResources().getString(R.string.unable_to_pin_shortcut), Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.show();
    }



    public static class AppObject {
        public final NvApp app;
        public boolean isRunning;
        public boolean isHidden;

        public AppObject(NvApp app) {
            if (app == null) {
                throw new IllegalArgumentException("app must not be null");
            }
            this.app = app;
        }

        @Override
        public String toString() {
            return app.getAppName();
        }
    }

    /**
     * 更新应用列表：对比现有的 appGridAdapter 中的 AppObject，与新的 appList 进行增补和移除。
     * 如果发现有更新，则调用 appGridAdapter.notifyDataSetChanged() 刷新界面。
     */
    private void updateUiWithAppList(final List<NvApp> appList) {
        AppView.this.runOnUiThread(() -> {
            boolean updated = false;

            // 先处理更新和新增
            for (NvApp app : appList) {
                boolean foundExistingApp = false;
                for (int i = 0; i < appGridAdapter.getCount(); i++) {
                    AppObject existingApp = (AppObject) appGridAdapter.getItem(i);
                    if (existingApp.app.getAppId() == app.getAppId()) {
                        // 如果名称变了，则更新
                        if (!existingApp.app.getAppName().equals(app.getAppName())) {
                            existingApp.app.setAppName(app.getAppName());
                            updated = true;
                        }
                        foundExistingApp = true;
                        break;
                    }
                }
                // 如果该应用不存在，则添加到列表中
                if (!foundExistingApp) {
                    appGridAdapter.addApp(new AppObject(app));
                    updated = true;
                }
            }

            // 再处理已被移除的应用
            int i = 0;
            while (i < appGridAdapter.getCount()) {
                AppObject existingApp = (AppObject) appGridAdapter.getItem(i);
                boolean foundInNewList = false;

                for (NvApp newApp : appList) {
                    if (existingApp.app.getAppId() == newApp.getAppId()) {
                        foundInNewList = true;
                        break;
                    }
                }
                if (!foundInNewList) {
                    appGridAdapter.removeApp(existingApp);
                    updated = true;
                    // 移除后不 ++i，因为列表内容已经发生变化
                    continue;
                }
                i++;
            }

            if (updated) {
                appGridAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * 根据服务器信息更新当前正在运行的应用（如果有），并刷新界面。
     */
    private void updateUiWithServerinfo(final ComputerDetails details) {
        AppView.this.runOnUiThread(() -> {
            boolean updated = false;
            // 遍历现有列表，判断哪一个（如果有）正处于运行状态
            for (int i = 0; i < appGridAdapter.getCount(); i++) {
                AppObject existingApp = (AppObject) appGridAdapter.getItem(i);

                // 如果这个应用本来就在运行并且与服务器 runningGameId 相同，则无需改动
                if (existingApp.isRunning &&
                        existingApp.app.getAppId() == details.runningGameId) {
                    return; // 直接结束
                }
                // 如果该应用 ID == runningGameId，但之前没标记为运行，就更新为运行
                else if (existingApp.app.getAppId() == details.runningGameId) {
                    existingApp.isRunning = true;
                    updated = true;
                }
                // 如果某个应用之前标记为运行，但现在服务器 runningId 已变，则取消其运行标记
                else if (existingApp.isRunning) {
                    existingApp.isRunning = false;
                    updated = true;
                }
            }

            if (updated) {
                appGridAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getAdapterFragmentLayoutId() {
        return PreferenceConfiguration.readPreferences(AppView.this).smallIconMode ?
                R.layout.app_grid_view_small : R.layout.app_grid_view;
    }
}
