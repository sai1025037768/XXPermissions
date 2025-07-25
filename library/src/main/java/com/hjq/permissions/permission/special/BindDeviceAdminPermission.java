package com.hjq.permissions.permission.special;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.hjq.permissions.manifest.AndroidManifestInfo;
import com.hjq.permissions.manifest.node.BroadcastReceiverManifestInfo;
import com.hjq.permissions.manifest.node.PermissionManifestInfo;
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
 *    time   : 2025/06/15
 *    desc   : 设备管理器权限类
 */
public final class BindDeviceAdminPermission extends SpecialPermission {

    public static final Creator<BindDeviceAdminPermission> CREATOR = new Creator<BindDeviceAdminPermission>() {

        @Override
        public BindDeviceAdminPermission createFromParcel(Parcel source) {
            return new BindDeviceAdminPermission(source);
        }

        @Override
        public BindDeviceAdminPermission[] newArray(int size) {
            return new BindDeviceAdminPermission[size];
        }
    };

    /** 设备管理器的 BroadcastReceiver 类名 */
    @NonNull
    private final String mDeviceAdminReceiverClassName;

    /** 申请设备管理器权限的附加说明 */
    @Nullable
    private final String mExtraAddExplanation;

    public BindDeviceAdminPermission(@NonNull Class<? extends DeviceAdminReceiver> deviceAdminReceiverClass, @Nullable String extraAddExplanation) {
        this(deviceAdminReceiverClass.getName(), extraAddExplanation);
    }

    public BindDeviceAdminPermission(@NonNull String deviceAdminReceiverClassName, @Nullable String extraAddExplanation) {
        mDeviceAdminReceiverClassName = deviceAdminReceiverClassName;
        mExtraAddExplanation = extraAddExplanation;
    }

    private BindDeviceAdminPermission(Parcel in) {
        this(in.readString(), in.readString());
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mDeviceAdminReceiverClassName);
        dest.writeString(mExtraAddExplanation);
    }

    @NonNull
    @Override
    public String getPermissionName() {
        return PermissionNames.BIND_DEVICE_ADMIN;
    }

    @Override
    public int getFromAndroidVersion() {
        return PermissionVersion.ANDROID_2_2;
    }

    @Override
    public boolean isGrantedPermission(@NonNull Context context, boolean skipRequest) {
        DevicePolicyManager devicePolicyManager;
        if (PermissionVersion.isAndroid6()) {
            devicePolicyManager = context.getSystemService(DevicePolicyManager.class);
        } else {
            devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        }
        // 虽然这个 SystemService 永远不为空，但是不怕一万，就怕万一，开展防御性编程
        if (devicePolicyManager == null) {
            return false;
        }
        return devicePolicyManager.isAdminActive(new ComponentName(context, mDeviceAdminReceiverClassName));
    }

    @NonNull
    @Override
    public List<Intent> getPermissionSettingIntents(@NonNull Context context, boolean skipRequest) {
        List<Intent> intentList = new ArrayList<>(2);
        Intent intent;

        intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(context, mDeviceAdminReceiverClassName));
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, mExtraAddExplanation);
        intentList.add(intent);

        intent = getAndroidSettingIntent();
        intentList.add(intent);

        return intentList;
    }

    @Override
    public void checkCompliance(@NonNull Activity activity, @NonNull List<IPermission> requestPermissions, @Nullable AndroidManifestInfo androidManifestInfo) {
        super.checkCompliance(activity, requestPermissions, androidManifestInfo);
        if (TextUtils.isEmpty(mDeviceAdminReceiverClassName)) {
            throw new IllegalArgumentException("Pass the BroadcastReceiverClass parameter as empty");
        }
        if (!PermissionUtils.isClassExist(mDeviceAdminReceiverClassName)) {
            throw new IllegalArgumentException("The passed-in " + mDeviceAdminReceiverClassName + " is an invalid class");
        }
    }

    @Override
    protected void checkSelfByManifestFile(@NonNull Activity activity,
                                            @NonNull List<IPermission> requestPermissions,
                                            @NonNull AndroidManifestInfo androidManifestInfo,
                                            @NonNull List<PermissionManifestInfo> permissionManifestInfoList,
                                            @Nullable PermissionManifestInfo currentPermissionManifestInfo) {
        super.checkSelfByManifestFile(activity, requestPermissions, androidManifestInfo, permissionManifestInfoList, currentPermissionManifestInfo);

        List<BroadcastReceiverManifestInfo> broadcastReceiverManifestInfoList = androidManifestInfo.broadcastReceiverManifestInfoList;
        for (BroadcastReceiverManifestInfo broadcastReceiverManifestInfo : broadcastReceiverManifestInfoList) {
            if (broadcastReceiverManifestInfo == null) {
                continue;
            }
            if (!PermissionUtils.reverseEqualsString(mDeviceAdminReceiverClassName, broadcastReceiverManifestInfo.name)) {
                // 不是目标的 BroadcastReceiver，继续循环
                continue;
            }
            if (broadcastReceiverManifestInfo.permission == null || !PermissionUtils.equalsPermission(this, broadcastReceiverManifestInfo.permission)) {
                // 这个 BroadcastReceiver 组件注册的 permission 节点为空或者错误
                throw new IllegalArgumentException("Please register permission node in the AndroidManifest.xml file, for example: "
                    + "<receiver android:name=\"" + mDeviceAdminReceiverClassName + "\" android:permission=\"" + getPermissionName() + "\" />");
            }
            return;
        }

        // 这个 BroadcastReceiver 组件没有在清单文件中注册
        throw new IllegalArgumentException("The \"" + mDeviceAdminReceiverClassName + "\" component is not registered in the AndroidManifest.xml file");
    }

    @NonNull
    public String getDeviceAdminReceiverClassName() {
        return mDeviceAdminReceiverClassName;
    }

    @Nullable
    public String getExtraAddExplanation() {
        return mExtraAddExplanation;
    }
}