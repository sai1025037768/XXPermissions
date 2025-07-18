package com.hjq.permissions;

import android.app.Activity;
import android.support.annotation.NonNull;
import com.hjq.permissions.permission.base.IPermission;
import java.util.List;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/XXPermissions
 *    time   : 2025/05/30
 *    desc   : 权限说明接口
 */
public interface OnPermissionDescription {

    /**
     * 询问是否要发起权限请求
     *
     * @param activity                      Activity 对象
     * @param requestPermissions            请求的权限
     * @param continueRequestRunnable       继续请求任务对象
     * @param breakRequestRunnable          中断请求任务对象
     */
    void askWhetherRequestPermission(@NonNull Activity activity,
                                    @NonNull List<IPermission> requestPermissions,
                                    @NonNull Runnable continueRequestRunnable,
                                    @NonNull Runnable breakRequestRunnable);

    /**
     * 权限请求开始
     *
     * @param activity                      Activity 对象
     * @param requestPermissions            请求的权限
     */
    void onRequestPermissionStart(@NonNull Activity activity, @NonNull List<IPermission> requestPermissions);

    /**
     * 权限请求结束
     *
     * @param activity                      Activity 对象
     * @param requestPermissions            请求的权限
     */
    void onRequestPermissionEnd(@NonNull Activity activity, @NonNull List<IPermission> requestPermissions);
}