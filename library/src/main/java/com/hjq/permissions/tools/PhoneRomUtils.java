package com.hjq.permissions.tools;

import android.annotation.SuppressLint;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/XXPermissions
 *    time   : 2023/04/05
 *    desc   : 厂商 Rom 工具类
 */
public final class PhoneRomUtils {

    private static final String[] ROM_HUAWEI    = {"huawei"};
    private static final String[] ROM_VIVO      = {"vivo"};
    private static final String[] ROM_XIAOMI    = {"xiaomi"};
    private static final String[] ROM_OPPO      = {"oppo"};
    private static final String[] ROM_LEECO     = {"leeco", "letv"};
    private static final String[] ROM_360       = {"360", "qiku"};
    private static final String[] ROM_ZTE       = {"zte"};
    private static final String[] ROM_ONEPLUS   = {"oneplus"};
    private static final String[] ROM_NUBIA     = {"nubia"};
    private static final String[] ROM_SAMSUNG = {"samsung"};
    private static final String[] ROM_HONOR = {"honor"};
    private static final String[] ROM_SMARTISAN = {"smartisan"};

    private static final String ROM_NAME_MIUI = "ro.miui.ui.version.name";
    private static final String ROM_NAME_HYPER_OS = "ro.mi.os.version.name";

    private static final String VERSION_PROPERTY_HUAWEI  = "ro.build.version.emui";
    private static final String VERSION_PROPERTY_VIVO    = "ro.vivo.os.build.display.id";

    /**
     * 小米手机有两种 Os 系统，一种是澎湃，另外一种是 miui
     *
     * 系统为了兼容，澎湃有的属性，miui 肯定有，反过来就没有，所以这里要把澎湃的版本号属性放在首位
     *
     * [ro.mi.os.version.incremental]: [OS1.0.7.0.UOQCNXM]
     * [ro.build.version.incremental]: [V13.0.12.0.SLCCNXM]
     *
     * 切记不要拿 ro.build.version.incremental 属性来获取澎湃的系统版本，否则有问题，
     * 1. 澎湃 1.0 会返回 [ro.build.version.incremental]: [V816.0.7.0.UOQCNXM]
     * 2. 澎湃 2.0 会返回 [ro.build.version.incremental]: [OS2.0.112.0.VNCCNXM]
     */
    private static final String[] VERSION_PROPERTY_XIAOMI  = {"ro.mi.os.version.incremental", "ro.build.version.incremental"};
    private static final String[] VERSION_PROPERTY_OPPO  = {"ro.build.version.opporom", "ro.build.version.oplusrom.display"};
    private static final String VERSION_PROPERTY_LEECO   = "ro.letv.release.version";
    private static final String VERSION_PROPERTY_360     = "ro.build.uiversion";
    private static final String VERSION_PROPERTY_ZTE     = "ro.build.MiFavor_version";
    private static final String VERSION_PROPERTY_ONEPLUS = "ro.rom.version";
    private static final String VERSION_PROPERTY_NUBIA   = "ro.build.rom.id";

    /**
     * 经过测试，得出以下结论
     * Magic 7.0 存放系统版本的属性是 msc.config.magic.version，
     * Magic 4.0 和 Magic 4.1 用的是 ro.build.version.magic 属性
     */
    private static final String[] VERSION_PROPERTY_MAGIC = {"msc.config.magic.version", "ro.build.version.magic"};

    private PhoneRomUtils() {}

    /**
     * 判断当前厂商系统是否为 emui
     */
    public static boolean isEmui() {
        return !TextUtils.isEmpty(PermissionUtils.getSystemPropertyValue(VERSION_PROPERTY_HUAWEI));
    }

    /**
     * 判断当前厂商系统是否为澎湃系统
     */
    public static boolean isHyperOs() {
        return !TextUtils.isEmpty(PermissionUtils.getSystemPropertyValue(ROM_NAME_HYPER_OS));
    }

    /**
     * 判断当前厂商系统是否为 miui
     */
    public static boolean isMiui() {
        if (isHyperOs()) {
            // 需要注意的是：该逻辑需要在判断 miui 系统之前判断，因为在 HyperOs 系统上面判断当前系统是否为 miui 系统也会返回 true
            // 这是因为 HyperOs 系统本身就是从 miui 系统演变而来，有这个问题也很正常，主要是厂商为了系统兼容性而保留的
            return false;
        }
        return !TextUtils.isEmpty(PermissionUtils.getSystemPropertyValue(ROM_NAME_MIUI));
    }

    /**
     * 判断当前厂商系统是否为 ColorOs
     */
    public static boolean isColorOs() {
        for (String property : VERSION_PROPERTY_OPPO) {
            String versionName = PermissionUtils.getSystemPropertyValue(property);
            if (TextUtils.isEmpty(versionName)) {
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * 判断当前厂商系统是否为 OriginOS
     */
    public static boolean isOriginOs() {
        return !TextUtils.isEmpty(PermissionUtils.getSystemPropertyValue(VERSION_PROPERTY_VIVO));
    }

    /**
     * 判断当前厂商系统是否为 OneUI
     */
    @SuppressLint("PrivateApi")
    public static boolean isOneUi() {
        return isRightRom(getBrand(), getManufacturer(), ROM_SAMSUNG);
        // 暂时无法通过下面的方式判断是否为 OneUI，只能通过品牌和机型来判断
        // https://stackoverflow.com/questions/60122037/how-can-i-detect-samsung-one-ui
//      try {
//         Field semPlatformIntField = Build.VERSION.class.getDeclaredField("SEM_PLATFORM_INT");
//         semPlatformIntField.setAccessible(true);
//         int semPlatformVersion = semPlatformIntField.getInt(null);
//         return semPlatformVersion >= 100000;
//      } catch (NoSuchFieldException  e) {
//         e.printStackTrace();
//         return false;
//      } catch (IllegalAccessException e) {
//         e.printStackTrace();
//         return false;
//      }
    }

    /**
     * 判断当前是否为鸿蒙系统
     */
    public static boolean isHarmonyOs() {
        // 鸿蒙系统没有 Android 10 以下的
        if (!PermissionVersion.isAndroid10()) {
            return false;
        }
        try {
            Class<?> buildExClass = Class.forName("com.huawei.system.BuildEx");
            Object osBrand = buildExClass.getMethod("getOsBrand").invoke(buildExClass);
            return "Harmony".equalsIgnoreCase(String.valueOf(osBrand));
        } catch (ClassNotFoundException ignore) {
            // 如果是类找不到的问题，就不打印日志，否则会影响看 Logcat 的体验
            // 相关 Github issue 地址：https://github.com/getActivity/XXPermissions/issues/368
            return false;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return false;
        }
    }

    /**
     * 判断当前是否为 MagicOs 系统（荣耀）
     */
    public static boolean isMagicOs() {
        return isRightRom(getBrand(), getManufacturer(), ROM_HONOR);
    }

    /**
     * 判断当前是否为 SmartisanOS 系统（锤子手机的系统）
     */
    public static boolean isSmartisanOS() {
        return isRightRom(getBrand(), getManufacturer(), ROM_SMARTISAN);
    }

    /**
     * 判断小米是否开启了系统优化（默认开启）
     *
     * Miui 关闭步骤为：开发者选项-> 启动 MIUI 优化 -> 点击关闭
     * 澎湃的关闭步骤为：开发者选项-> 启用系统优化 -> 点击关闭
     *
     * 需要注意的是，关闭优化后，可以跳转到小米定制的权限请求页面，但是开启权限仍然是没有效果的
     * 另外关于 miui 国际版开发者选项中是没有优化选项的，但是代码判断是有开启优化选项，也就是默认开启，这样是正确的
     * 相关 Github issue 地址：https://github.com/getActivity/XXPermissions/issues/38
     */
    @SuppressLint("PrivateApi")
    public static boolean isXiaomiSystemOptimization() {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method getMethod = clazz.getMethod("get", String.class, String.class);
            String ctsValue = String.valueOf(getMethod.invoke(clazz, "ro.miui.cts", ""));
            Method getBooleanMethod = clazz.getMethod("getBoolean", String.class, boolean.class);
            return Boolean.parseBoolean(
                String.valueOf(getBooleanMethod.invoke(clazz, "persist.sys.miui_optimization", !"1".equals(ctsValue))));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * 获取厂商系统版本的大版本号
     *
     * @return               如果获取不到则返回 0
     */
    public static int getRomBigVersionCode() {
        String romVersionName = PhoneRomUtils.getRomVersionName();
        if (romVersionName == null) {
            return 0;
        }
        String[] array = romVersionName.split("\\.");
        if (array.length == 0) {
            return 0;
        }
        try {
           return Integer.parseInt(array[0]);
        } catch (Exception e) {
            // java.lang.NumberFormatException: Invalid int: "0 "
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 返回经过美化的厂商系统版本号
     */
    @Nullable
    public static String getRomVersionName() {
        String originalRomVersionName = getOriginalRomVersionName();

        if (TextUtils.isEmpty(originalRomVersionName)) {
            return null;
        }

        // 使用正则表达式匹配数字和点号组成的版本号
        Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)+)");
        Matcher matcher = pattern.matcher(originalRomVersionName);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * 返回原始的厂商系统版本号
     */
    @Nullable
    public static String getOriginalRomVersionName() {
        final String brand = getBrand();
        final String manufacturer = getManufacturer();
        if (isRightRom(brand, manufacturer, ROM_HUAWEI)) {
            String version = PermissionUtils.getSystemPropertyValue(VERSION_PROPERTY_HUAWEI);
            String[] temp = version.split("_");
            if (temp.length > 1) {
                return temp[1];
            } else {
                // 需要注意的是 华为畅享 5S Android 5.1 获取到的厂商版本号是 EmotionUI 3，而不是 3.1 或者 3.0 这种
                if (version.contains("EmotionUI")) {
                    return version.replaceFirst("EmotionUI\\s*", "");
                }
                return version;
            }
        }
        if (isRightRom(brand, manufacturer, ROM_VIVO)) {
            // 需要注意的是 vivo iQOO 9 Pro Android 12 获取到的厂商版本号是 OriginOS Ocean
            return PermissionUtils.getSystemPropertyValue(VERSION_PROPERTY_VIVO);
        }
        if (isRightRom(brand, manufacturer, ROM_XIAOMI)) {
            for (String property : VERSION_PROPERTY_XIAOMI) {
                String versionName = PermissionUtils.getSystemPropertyValue(property);
                if (TextUtils.isEmpty(property)) {
                    continue;
                }
                return versionName;
            }
            return "";
        }
        if (isRightRom(brand, manufacturer, ROM_OPPO)) {
            for (String property : VERSION_PROPERTY_OPPO) {
                String versionName = PermissionUtils.getSystemPropertyValue(property);
                if (TextUtils.isEmpty(property)) {
                    continue;
                }
                return versionName;
            }
            return "";
        }
        if (isRightRom(brand, manufacturer, ROM_LEECO)) {
            return PermissionUtils.getSystemPropertyValue(VERSION_PROPERTY_LEECO);
        }

        if (isRightRom(brand, manufacturer, ROM_360)) {
            return PermissionUtils.getSystemPropertyValue(VERSION_PROPERTY_360);
        }
        if (isRightRom(brand, manufacturer, ROM_ZTE)) {
            return PermissionUtils.getSystemPropertyValue(VERSION_PROPERTY_ZTE);
        }
        if (isRightRom(brand, manufacturer, ROM_ONEPLUS)) {
            return PermissionUtils.getSystemPropertyValue(VERSION_PROPERTY_ONEPLUS);
        }
        if (isRightRom(brand, manufacturer, ROM_NUBIA)) {
            return PermissionUtils.getSystemPropertyValue(VERSION_PROPERTY_NUBIA);
        }
        if (isRightRom(brand, manufacturer, ROM_HONOR)) {
            for (String property : VERSION_PROPERTY_MAGIC) {
                String versionName = PermissionUtils.getSystemPropertyValue(property);
                if (TextUtils.isEmpty(property)) {
                    continue;
                }
                return versionName;
            }
            return "";
        }

        return PermissionUtils.getSystemPropertyValue("");
    }

    private static boolean isRightRom(final String brand, final String manufacturer, final String... names) {
        for (String name : names) {
            if (brand.contains(name) || manufacturer.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private static String getBrand() {
        return Build.BRAND.toLowerCase();
    }

    private static String getManufacturer() {
        return Build.MANUFACTURER.toLowerCase();
    }
}