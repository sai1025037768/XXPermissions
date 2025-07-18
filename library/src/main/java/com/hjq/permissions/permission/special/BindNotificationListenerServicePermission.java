package com.hjq.permissions.permission.special;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.hjq.permissions.manifest.AndroidManifestInfo;
import com.hjq.permissions.manifest.node.PermissionManifestInfo;
import com.hjq.permissions.manifest.node.ServiceManifestInfo;
import com.hjq.permissions.permission.PermissionNames;
import com.hjq.permissions.permission.base.IPermission;
import com.hjq.permissions.permission.common.SpecialPermission;
import com.hjq.permissions.tools.PermissionVersion;
import com.hjq.permissions.tools.PermissionUtils;
import java.util.ArrayList;
import java.util.List;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/XXPermissions
 *    time   : 2025/06/11
 *    desc   : 通知栏监听权限类
 */
public final class BindNotificationListenerServicePermission extends SpecialPermission {

    /** 当前权限名称，注意：该常量字段仅供框架内部使用，不提供给外部引用，如果需要获取权限名称的字符串，请直接通过 {@link PermissionNames} 类获取 */
    public static final String PERMISSION_NAME = PermissionNames.BIND_NOTIFICATION_LISTENER_SERVICE;

    public static final Parcelable.Creator<BindNotificationListenerServicePermission> CREATOR = new Parcelable.Creator<BindNotificationListenerServicePermission>() {

        @Override
        public BindNotificationListenerServicePermission createFromParcel(Parcel source) {
            return new BindNotificationListenerServicePermission(source);
        }

        @Override
        public BindNotificationListenerServicePermission[] newArray(int size) {
            return new BindNotificationListenerServicePermission[size];
        }
    };

    /** Settings.Secure.ENABLED_NOTIFICATION_LISTENERS */
    private static final String SETTING_ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

    /** 通知监听器的 Service 类名 */
    @NonNull
    private final String mNotificationListenerServiceClassName;

    public BindNotificationListenerServicePermission(@NonNull Class<? extends NotificationListenerService> notificationListenerServiceClass) {
        this(notificationListenerServiceClass.getName());
    }

    public BindNotificationListenerServicePermission(@NonNull String notificationListenerServiceClassName) {
        mNotificationListenerServiceClassName = notificationListenerServiceClassName;
    }

    private BindNotificationListenerServicePermission(Parcel in) {
        this(in.readString());
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mNotificationListenerServiceClassName);
    }

    @NonNull
    @Override
    public String getPermissionName() {
        return PERMISSION_NAME;
    }

    @Override
    public int getFromAndroidVersion() {
        return PermissionVersion.ANDROID_4_3;
    }

    @Override
    public boolean isGrantedPermission(@NonNull Context context, boolean skipRequest) {
        // 经过实践得出，通知监听权限是在 Android 4.3 才出现的，所以前面的版本统一返回 true
        if (!PermissionVersion.isAndroid4_3()) {
            return true;
        }
        NotificationManager notificationManager;
        if (PermissionVersion.isAndroid6()) {
            notificationManager = context.getSystemService(NotificationManager.class);
        } else {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        String serviceClassName = PermissionUtils.isClassExist(mNotificationListenerServiceClassName) ?
                                    mNotificationListenerServiceClassName : null;
        // 虽然这个 SystemService 永远不为空，但是不怕一万，就怕万一，开展防御性编程
        if (PermissionVersion.isAndroid8_1() && notificationManager != null && serviceClassName != null) {
            return notificationManager.isNotificationListenerAccessGranted(new ComponentName(context, serviceClassName));
        }
        final String enabledNotificationListeners = Settings.Secure.getString(context.getContentResolver(), SETTING_ENABLED_NOTIFICATION_LISTENERS);
        if (TextUtils.isEmpty(enabledNotificationListeners)) {
            return false;
        }
        // com.hjq.permissions.demo/com.hjq.permissions.demo.NotificationMonitorService:com.huawei.health/com.huawei.bone.ui.setting.NotificationPushListener
        final String[] allComponentNameArray = enabledNotificationListeners.split(":");
        for (String component : allComponentNameArray) {
            ComponentName componentName = ComponentName.unflattenFromString(component);
            if (componentName == null) {
                continue;
            }
            if (serviceClassName != null) {
                // 精准匹配
                if (serviceClassName.equals(componentName.getClassName())) {
                    return true;
                }
            } else {
                // 模糊匹配
                if (context.getPackageName().equals(componentName.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @NonNull
    @Override
    public List<Intent> getPermissionSettingIntents(@NonNull Context context, boolean skipRequest) {
        List<Intent> intentList = new ArrayList<>(3);
        Intent intent;

        if (PermissionVersion.isAndroid11() && PermissionUtils.isClassExist(mNotificationListenerServiceClassName)) {
            intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);
            intent.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                            new ComponentName(context, mNotificationListenerServiceClassName).flattenToString());
            intentList.add(intent);
        }

        String action;
        if (PermissionVersion.isAndroid5_1()) {
            action = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;
        } else {
            // android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            action = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
        }
        intent = new Intent(action);
        intentList.add(intent);

        intent = getAndroidSettingIntent();
        intentList.add(intent);

        return intentList;
    }

    @Override
    public void checkCompliance(@NonNull Activity activity, @NonNull List<IPermission> requestPermissions, @Nullable AndroidManifestInfo androidManifestInfo) {
        super.checkCompliance(activity, requestPermissions, androidManifestInfo);
        if (TextUtils.isEmpty(mNotificationListenerServiceClassName)) {
            throw new IllegalArgumentException("Pass the ServiceClass parameter as empty");
        }
        if (!PermissionUtils.isClassExist(mNotificationListenerServiceClassName)) {
            throw new IllegalArgumentException("The passed-in " + mNotificationListenerServiceClassName + " is an invalid class");
        }
    }

    @Override
    protected void checkSelfByManifestFile(@NonNull Activity activity,
                                            @NonNull List<IPermission> requestPermissions,
                                            @NonNull AndroidManifestInfo androidManifestInfo,
                                            @NonNull List<PermissionManifestInfo> permissionManifestInfoList,
                                            @Nullable PermissionManifestInfo currentPermissionManifestInfo) {
        super.checkSelfByManifestFile(activity, requestPermissions, androidManifestInfo, permissionManifestInfoList, currentPermissionManifestInfo);

        List<ServiceManifestInfo> serviceManifestInfoList = androidManifestInfo.serviceManifestInfoList;
        for (ServiceManifestInfo serviceManifestInfo : serviceManifestInfoList) {
            if (serviceManifestInfo == null) {
                continue;
            }
            if (!PermissionUtils.reverseEqualsString(mNotificationListenerServiceClassName, serviceManifestInfo.name)) {
                // 不是目标的 Service，继续循环
                continue;
            }
            if (serviceManifestInfo.permission == null || !PermissionUtils.equalsPermission(this, serviceManifestInfo.permission)) {
                // 这个 Service 组件注册的 permission 节点为空或者错误
                throw new IllegalArgumentException("Please register permission node in the AndroidManifest.xml file, for example: "
                    + "<service android:name=\"" + mNotificationListenerServiceClassName + "\" android:permission=\"" + getPermissionName() + "\" />");
            }
            return;
        }

        // 这个 Service 组件没有在清单文件中注册
        throw new IllegalArgumentException("The \"" + mNotificationListenerServiceClassName + "\" component is not registered in the AndroidManifest.xml file");
    }

    @NonNull
    public String getNotificationListenerServiceClassName() {
        return mNotificationListenerServiceClassName;
    }
}