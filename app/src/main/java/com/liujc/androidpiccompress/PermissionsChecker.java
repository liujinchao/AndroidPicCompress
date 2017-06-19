package com.liujc.androidpiccompress;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

/**
 * 类名称：PermissionsChecker
 * 创建者：Create by liujc
 * 创建时间：Create on 2016/12/2 12:44
 * 描述：针对android 6.0 动态获取权限，检查权限的工具类
 */
public class PermissionsChecker {
    private final Context mContext;

    public PermissionsChecker(Context context) {
        mContext = context.getApplicationContext();
    }

    // 判断权限集合
    public boolean lacksPermissions(String... permissions) {
        for (String permission : permissions) {
            if (lacksPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    // 判断是否缺少权限
    private boolean lacksPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(mContext, permission) ==
                    PackageManager.PERMISSION_DENIED;
        }
        return false;
    }

}
