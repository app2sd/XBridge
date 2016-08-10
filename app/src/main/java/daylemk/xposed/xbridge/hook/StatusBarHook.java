package daylemk.xposed.xbridge.hook;

import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import daylemk.xposed.xbridge.R;
import daylemk.xposed.xbridge.action.Action;
import daylemk.xposed.xbridge.action.AppInfoAction;
import daylemk.xposed.xbridge.action.AppOpsAction;
import daylemk.xposed.xbridge.action.AppSettingsAction;
import daylemk.xposed.xbridge.action.ClipBoardAction;
import daylemk.xposed.xbridge.action.LightningWallAction;
import daylemk.xposed.xbridge.action.MyAndroidToolsAction;
import daylemk.xposed.xbridge.action.NotifyCleanAction;
import daylemk.xposed.xbridge.action.PlayAction;
import daylemk.xposed.xbridge.action.SearchAction;
import daylemk.xposed.xbridge.action.XHaloFloatingWindowAction;
import daylemk.xposed.xbridge.action.XPrivacyAction;
import daylemk.xposed.xbridge.data.OnPreferenceChangedReceiver;
import daylemk.xposed.xbridge.data.StaticData;
import daylemk.xposed.xbridge.ui.SizeInputFragment;
import daylemk.xposed.xbridge.utils.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Status bar hook
 * Created by DayLemK on 2015/4/30.
 */
public class StatusBarHook extends Hook {
    public static final String TAG = "StatusBarHook";
    public static final String PKG_KEY_GUARD = "com.android.keyguard";

    // TODO: need get this value from original class
    public static final int FLAG_EXCLUDE_NONE = 0;

    // set the field and we can use it later to collapse notification or unlock screen
    private Object statusBarObject = null;
    private Context context = null;
    private PackageManager packageManager = null;
    // the OnDismissActionInterface
    private Class<?> onDismissActionInterface = null;
    /**
     * notification guts id show always be the same, so, just get once
     */
    private int idGuts = -1;
    /**
     * inspect item layout params should get once too
     */
    private LinearLayout.LayoutParams inspectLayoutParams;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        super.initZygote(startupParam);

//        inspectItemButton = sModuleRes.fwd(R.layout.notification_inspect_item);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws
            Throwable {
        super.handleLoadPackage(loadPackageParam);
        Log.v(TAG, "enter the status bar hook");
        Class<?> systemUIAppClass = XposedHelpers.findClass(StaticData.PKG_NAME_SYSTEMUI +
                        ".SystemUIApplication",
                loadPackageParam
                        .classLoader);
        XposedBridge.hookAllMethods(systemUIAppClass, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Log.d(TAG, "SystemUIApplication onCreate hook");
                Application app = (Application) param.thisObject;
                Log.d(TAG, "app: " + app);
                app.registerReceiver(new OnPreferenceChangedReceiver(), new IntentFilter
                        (StaticData.ACTION_PREFERENCE_CHANGED));
            }
        });

        final Class<?> baseStatusBarClass = XposedHelpers.findClass(StaticData.PKG_NAME_SYSTEMUI +
                ".statusbar.BaseStatusBar", loadPackageParam.classLoader);
        Log.d(TAG, "BaseStatusBar: " + baseStatusBarClass);
        String methodName = "inflateGuts";
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            // Why change the name of method ???
            methodName = "bindGuts";
        }
        XposedBridge.hookAllMethods(baseStatusBarClass, methodName, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!Action.isActionsShowInStatusBar()) {
                    // if none of this is showed here, do nothing
                    return;
                }

                Log.v(TAG, "after inflateGuts method hooked");
                super.afterHookedMethod(param);
                // set the statusBar everytime
                if (!param.thisObject.equals(statusBarObject)) {
                    Log.w(TAG, "statusBar is different: " + statusBarObject);
                    statusBarObject = param.thisObject;
                    context = (Context) XposedHelpers.getObjectField
                            (statusBarObject, "mContext");
                    Log.d(TAG, "context: " + context);
                }
                final Resources res = context.getResources();

                // 1) get the notification guts id
                if (idGuts == -1) {
                    // ignore: and inspect item id
                    // EDIT: get guts view here
                    idGuts = res.getIdentifier("notification_guts", "id",
                            StaticData.PKG_NAME_SYSTEMUI);
                }
                Log.d(TAG, "guts id: " + idGuts);

                // move get guts view here, so we can check if the action is already injected or not
                final Object expandNotiRowObject = param.args[0];
                Log.d(TAG, "expandNotiRowObject: " + expandNotiRowObject);
                final FrameLayout layoutGuts = (FrameLayout) XposedHelpers.callMethod
                        (expandNotiRowObject, "findViewById", idGuts);
                Log.d(TAG, "get the guts view: " + layoutGuts);
                Log.d(TAG, "guts children number: " + layoutGuts.getChildCount());
                // check if the view already exists
                // EDIT: we should use the dynamic generated id, 'cause that will not be equal
                // with another package static id

                boolean isPlayNeed2Add = false;
                boolean isOpsNeed2Add = false;
                boolean isAppSetNeed2Add = false;
                boolean isClipBoardNeed2Add = false;
                boolean isSearchNeed2Add = false;
                boolean isXPrivacyNeed2Add = false;
                boolean isAppInfoNeed2Add = false;
                boolean isNotifyCleanNeed2Add = false;
                boolean isLightningWallNeed2Add = false;
                boolean isXHaloFloatingWinNeed2Add = false;
                boolean isMyAndroidToolsNeed2Add = false;
                // check if need to add action
                if (PlayAction.isShow && PlayAction.isShowInStatusBar) {
                    isPlayNeed2Add = Action.isNeed2Add(layoutGuts, PlayAction.class);
                }
                if (AppOpsAction.isShow && AppOpsAction.isShowInStatusBar) {
                    isOpsNeed2Add = Action.isNeed2Add(layoutGuts, AppOpsAction.class);
                }
                if (AppSettingsAction.isShow && AppSettingsAction.isShowInStatusBar) {
                    isAppSetNeed2Add = Action.isNeed2Add(layoutGuts, AppSettingsAction.class);
                }
                if (ClipBoardAction.isShow && ClipBoardAction.isShowInStatusBar) {
                    isClipBoardNeed2Add = Action.isNeed2Add(layoutGuts, ClipBoardAction.class);
                }
                if (SearchAction.isShow && SearchAction.isShowInStatusBar) {
                    isSearchNeed2Add = Action.isNeed2Add(layoutGuts, SearchAction.class);
                }
                if (XPrivacyAction.isShow && XPrivacyAction.isShowInStatusBar) {
                    isXPrivacyNeed2Add = Action.isNeed2Add(layoutGuts, XPrivacyAction.class);
                }
                if (AppInfoAction.isShow && AppInfoAction.isShowInStatusBar) {
                    isAppInfoNeed2Add = Action.isNeed2Add(layoutGuts, AppInfoAction.class);
                }
                if (NotifyCleanAction.isShow && NotifyCleanAction.isShowInStatusBar) {
                    isNotifyCleanNeed2Add = Action.isNeed2Add(layoutGuts, NotifyCleanAction.class);
                }
                if (LightningWallAction.isShow && LightningWallAction.isShowInStatusBar) {
                    isLightningWallNeed2Add = Action.isNeed2Add(layoutGuts, LightningWallAction
                            .class);
                }
                if (XHaloFloatingWindowAction.isShow && XHaloFloatingWindowAction
                        .isShowInStatusBar) {
                    isXHaloFloatingWinNeed2Add = Action.isNeed2Add(layoutGuts,
                            XHaloFloatingWindowAction.class);
                }
                if (MyAndroidToolsAction.isShow && MyAndroidToolsAction
                        .isShowInStatusBar) {
                    isMyAndroidToolsNeed2Add = Action.isNeed2Add(layoutGuts,
                            MyAndroidToolsAction.class);
                }

                if (!(isPlayNeed2Add || isOpsNeed2Add || isAppSetNeed2Add || isClipBoardNeed2Add
                        || isSearchNeed2Add || isXPrivacyNeed2Add || isAppInfoNeed2Add ||
                        isNotifyCleanNeed2Add || isXHaloFloatingWinNeed2Add ||
                        isMyAndroidToolsNeed2Add)) {
                    Log.d(TAG, "need add nothing");
                    return;
                }
                // here, we do some prepare for the button general stuff, like get package name,
                // get layout params etc.

                // here we just need layout params
                getInspectLayoutParams(res, layoutGuts);

                // 3) get the package name
                Object statusBarNotificationObject = XposedHelpers.callMethod(expandNotiRowObject,
                        "getStatusBarNotification");
                Log.d(TAG, "statusBarNotificationObject: " + statusBarNotificationObject);
                final String pkgName = (String) XposedHelpers.callMethod
                        (statusBarNotificationObject, "getPackageName");
                Log.i(TAG, "package name: " + pkgName);

                // 4) init the XBridge button
                // the first child is a linearLayout in the aosp
                final LinearLayout linearLayout = (LinearLayout) layoutGuts.getChildAt(0);
                Log.d(TAG, "linear layout: " + linearLayout);

                // get package manager
                // no need
                /*final int userId = (int) XposedHelpers.callMethod(XposedHelpers.callMethod
                        (statusBarNotificationObject, "getUser"), "getIdentifier");
                Log.d(TAG, "user id: " + userId);
                packageManager = (PackageManager) XposedHelpers.callMethod
                        (statusBarObject,
                                "getPackageManagerForUser",
                                userId);
                Log.d(TAG, "packageManager: " + packageManager);*/

                // 5) add the button to the guts layout
                if (isPlayNeed2Add) {
                    Action action = new PlayAction();
                    addViewAndSetAction(action, linearLayout, pkgName);
                }
                if (isOpsNeed2Add) {
                    Action action = new AppOpsAction();
                    addViewAndSetAction(action, linearLayout, pkgName);
                }
                if (isAppSetNeed2Add) {
                    Action action = new AppSettingsAction();
                    addViewAndSetAction(action, linearLayout, pkgName);
                }
                if (isClipBoardNeed2Add) {
                    Action action = new ClipBoardAction();
                    addViewAndSetAction(action, linearLayout, pkgName);
                }
                if (isSearchNeed2Add) {
                    Action action = new SearchAction();
                    addViewAndSetAction(action, linearLayout, pkgName);
                }
                if (isXPrivacyNeed2Add) {
                    Action action = new XPrivacyAction();
                    addViewAndSetAction(action, linearLayout, pkgName);
                }
                if (isAppInfoNeed2Add) {
                    Action action = new AppInfoAction();
                    addViewAndSetAction(action, linearLayout, pkgName);
                }
                if (isNotifyCleanNeed2Add) {
                    Action action = new NotifyCleanAction();
                    addViewAndSetAction(action, linearLayout, pkgName);
                }
                if (isLightningWallNeed2Add) {
                    Action action = new LightningWallAction();
                    addViewAndSetAction(action, linearLayout, pkgName);
                }
                if (isXHaloFloatingWinNeed2Add) {
                    Action action = new XHaloFloatingWindowAction();
                    addViewAndSetAction(action, linearLayout, pkgName);
                }
                if (isMyAndroidToolsNeed2Add) {
                    Action action = new MyAndroidToolsAction();
                    addViewAndSetAction(action, linearLayout, pkgName);
                }
            }
        });

        onDismissActionInterface = XposedHelpers.findClass(PKG_KEY_GUARD +
                ".KeyguardHostView.OnDismissAction", loadPackageParam.classLoader);
        Log.d(TAG, "onDismissActionInterface: " + onDismissActionInterface);

    }

    private void getInspectLayoutParams(Resources res, FrameLayout layoutGuts) {
        if (inspectLayoutParams == null) {
            final int idInspectItemId = res.getIdentifier("notification_inspect_item", "id",
                    StaticData.PKG_NAME_SYSTEMUI);
            Log.d(TAG, "inspect item id: " + idInspectItemId);

            // 2) get the guts view and inspect item button view
            final ImageButton imageButtonInspect = (ImageButton) layoutGuts.findViewById
                    (idInspectItemId);
            Log.d(TAG, "the inspect image button: " + imageButtonInspect);
            inspectLayoutParams = new LinearLayout.LayoutParams(imageButtonInspect
                    .getLayoutParams());
            // set the width to the 3/4 of the original width
            // EDIT: 3/4 is just fine
            //inspectLayoutParams.width = inspectLayoutParams.width * 3 / 2;
            // EDIT: use size
            inspectLayoutParams.width = inspectLayoutParams.width * SizeInputFragment.size / 100;
            Log.d(TAG, "image button inspect: " + StatusBarHook.this.inspectLayoutParams
                    .toString());
        }
    }

    private void addViewAndSetAction(Action action, LinearLayout linearLayout, String pkgName) {
        ImageButton xBridgeButton = createXBridgeButton(context, inspectLayoutParams);
        action.setAction(StatusBarHook.this, context, pkgName, xBridgeButton);
        // add the view to the last-1
        // EDIT: move to last one, 'cause some notification has more than one icon
        linearLayout.addView(xBridgeButton, linearLayout.getChildCount());
    }

    private ImageButton createXBridgeButton(Context context, ViewGroup.LayoutParams layoutParams) {
        // inflate layout from xBridge package name
        // EDIT: finally, this can be done
        Context xBridgeContext = Hook.getXBridgeContext(context);
        ImageButton xBridgeButton = (ImageButton) LayoutInflater.from(xBridgeContext)
                .inflate(R.layout
                        .notification_inspect_item, null);

        Log.d(TAG, "inflate image button: " + xBridgeButton);

        // set the style finally!!!
        // EDIT: set the style on the fly acts wired
        // copy the params from the inspect item button
        xBridgeButton.setLayoutParams(layoutParams);
        // set padding here, half of padding
        xBridgeButton.setPadding(xBridgeButton.getPaddingLeft() / 2, xBridgeButton.getPaddingTop
                () / 2, xBridgeButton.getPaddingRight() / 2, xBridgeButton.getPaddingBottom() / 2);

        return xBridgeButton;
    }

    public void dismissKeyguardAndStartAction(final OnDismissKeyguardAction
                                                      onDismissKeyguardAction) {
        if (statusBarObject == null) {
            Log.e(TAG, "baseStatusBar is null, give up");
            return;
        }
        Log.i(TAG, "dismiss keyguard");

        // dismiss and start intent
        // EDIT: super abstract private method can't be called?
//        Method method;
//        try {
//            method = statusBarObject.getClass().getSuperclass().getDeclaredMethod(
//                    ("startNotificationGutsIntent", Intent.class, Integer.class);
//            Log.d(TAG, "the method: " + method);
//            method.setAccessible(true);
//            method.invoke(statusBarObject, intent, appUid);
//        } catch (Exception e) {
//            XposedBridge.log(e);
//        }

        Object mStatusBarKeyguardViewManager = XposedHelpers.
                getObjectField(statusBarObject, "mStatusBarKeyguardViewManager");
        Log.d(TAG, "get the keyguardViewManager: " + mStatusBarKeyguardViewManager);
        final boolean keyguardShowing = (boolean) XposedHelpers.callMethod
                (mStatusBarKeyguardViewManager, "isShowing");
        Log.d(TAG, "keyguardShowing: " + keyguardShowing);

        Object onDismissAction = Proxy.newProxyInstance(onDismissActionInterface.getClassLoader()
                , new Class<?>[]{onDismissActionInterface}, new InvocationHandler() {

            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                Log.d(TAG, "invoke: " + method.getName());
                if (method.getName().equals("onDismiss")) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "keyguardShowing: " + keyguardShowing);
                            if (keyguardShowing) {
                                Class<?> activityManagerNativeClass = XposedHelpers.findClass
                                        ("android.app.ActivityManagerNative",
                                                statusBarObject.getClass().getClassLoader());
                                Log.d(TAG, "activityManagerNativeClass: " +
                                        activityManagerNativeClass);
                                if (activityManagerNativeClass != null) {
                                    Object defaultNative = XposedHelpers.callStaticMethod
                                            (activityManagerNativeClass, "getDefault");
                                    Log.d(TAG, "defaultNative: " + defaultNative);
                                    // call keyguardWaitingForActivityDrawn method
                                    XposedHelpers.callMethod(defaultNative,
                                            "keyguardWaitingForActivityDrawn");
                                }
                            }
                            // end of keyguard showing

                            // call the interface back
                            onDismissKeyguardAction.onDismissKeyguard();

                        }
                    });
                    Log.d(TAG, "collapse panels");
                    XposedHelpers.callMethod(statusBarObject, "animateCollapsePanels",
                            FLAG_EXCLUDE_NONE,
                            true /* force */);
                    return true;
                } else if (method.getName().equals("toString")) {
                    //TODO: invoke original method
                    return o.getClass().toString();
                }
                // else invoke the original method
                return method.invoke(o, objects);
            }
        });
        Log.d(TAG, "onDismissAction: " + onDismissAction);

        XposedHelpers.callMethod(statusBarObject, "dismissKeyguardThenExecute", onDismissAction,
                false/* afterKeyguardGone */);
    }

    public interface OnDismissKeyguardAction {
        void onDismissKeyguard();
    }
}
