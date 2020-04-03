package tw.idv.palatis.xwechatshortcut;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.util.Log;

import java.util.Collections;

import androidx.annotation.Keep;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static android.util.Log.getStackTraceString;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static tw.idv.palatis.xwechatshortcut.BuildConfig.APPLICATION_ID;

@Keep
public class HookMain implements IXposedHookLoadPackage {
    private static final String LOG_TAG = "XWeChatSC";

    private static final String WECHAT_PACKAGE_NAME = "com.tencent.mm";
    private static final String WECHAT_PROCESS_NAME = WECHAT_PACKAGE_NAME;
    private static final String WECHAT_LAUNCHER_UI_CLASS_NAME = WECHAT_PACKAGE_NAME + ".ui.LauncherUI";

    private static boolean sIsInitialized = false;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!WECHAT_PACKAGE_NAME.equals(lpparam.packageName))
            return;
        if (!WECHAT_PROCESS_NAME.equals(lpparam.processName))
            return;

        Log.d(LOG_TAG, "handleLoadPackage(): pkg = " + lpparam.packageName + ", process = " + lpparam.processName);

        findAndHookMethod(
                ContextWrapper.class,
                "attachBaseContext",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        try {
                            if (sIsInitialized)
                                return;
                            sIsInitialized = true;

                            final Context context = (Context) param.thisObject;
                            final ShortcutManager sm = context.getSystemService(ShortcutManager.class);
                            final PackageManager pm = context.getPackageManager();
                            if (sm == null || pm == null)
                                return;

                            final Resources res = pm.getResourcesForApplication(APPLICATION_ID);
                            final Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.setPackage(WECHAT_PACKAGE_NAME);
                            intent.setClassName(WECHAT_PACKAGE_NAME, WECHAT_LAUNCHER_UI_CLASS_NAME);
                            intent.putExtra("LauncherUI.From.Scaner.Shortcut", true);
                            intent.addCategory(Intent.CATEGORY_LAUNCHER);
                            final ShortcutInfo shortcut = new ShortcutInfo.Builder(context, "xwechatshortcut_" + BuildConfig.VERSION_CODE)
                                    .setShortLabel(res.getString(R.string.shortcut_scan_label_short))
                                    .setLongLabel(res.getString(R.string.shortcut_scan_label_long))
                                    .setIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_camera))
                                    .setIntent(intent)
                                    .build();
                            Log.d(LOG_TAG, "afterHookedMethod(): calling setDynamicShortcuts(): " + sm.setDynamicShortcuts(Collections.singletonList(shortcut)));
                            Log.d(LOG_TAG, "afterHookedMethod(): calling updateShortcuts(): " + sm.updateShortcuts(Collections.singletonList(shortcut)));
                        } catch (Throwable t) {
                            XposedBridge.log(LOG_TAG + ": err = " + getStackTraceString(t));
                            throw t;
                        }
                    }
                }
        );
    }
}
