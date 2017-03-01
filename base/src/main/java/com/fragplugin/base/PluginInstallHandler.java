package com.fragplugin.base;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.text.TextUtils;
import android.util.Log;

import com.fragplugin.base.core.NativeLibraryHelperCompat;
import com.fragplugin.base.update.PluginUpdateInfoList;
import com.fragplugin.base.utils.AppLog;

import org.apache.commons.io.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Lijj on 16/8/20.
 */
public class PluginInstallHandler {
    private static final String TAG = PluginInstallHandler.class.getSimpleName();

    public static final int INSTALL_SUCCEEDED = 1;
    public static final int INSTALL_FAILED_INTERNAL_ERROR = -110;
    public static final int INSTALL_FAILED_NOT_SUPPORT_ABI = -3;
    public static final int INSTALL_FAILED_HAS_LOAD = -4;

    private static final String FAILED_REASON_APKBAD = "ApkBad";
    private static final String FAILED_REASON_NOT_SUPPORT_ABI = "NotSupportAbi";

    private static final Map<String, byte[]> mLockMap = Collections.synchronizedMap(new HashMap<String, byte[]>());

    private static final String PREFIX_FOR_UPDATE = "update_";

    public static String generateUpdateFileName(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return "";
        }
        return PREFIX_FOR_UPDATE + fileName;
    }

    /**
     * 安装插件
     *
     * @param appContext
     */
    public static void installUpdate(Context appContext) {

        // 获得需要更新的插件文件集合
        List<File> updateFiles = getPluginsForUpdate(appContext);
        if (updateFiles == null) {
            return;
        }
        for (File file : updateFiles) {// 遍历需要更新的插件文件
            boolean flag = installUpdate(appContext, file);
            if(flag) {
                deleteAllVerOfPlugin(appContext, file);
            }
        }
    }

    /**
     * 在测试模式下，从assert目录中安装插件
     */
    static void installDebugPlugins(Context appContext) {
        try {
            String[] files = appContext.getAssets().list("");
            ArrayList<String> apkFiles = new ArrayList<>();
            for (String file : files) {
                if (file.toLowerCase().endsWith(".apk")) {
                    apkFiles.add(file);
                }
            }

            for (String apk : apkFiles) {
                installPlugin(appContext, appContext.getAssets().open(apk), apk.substring(0, apk.lastIndexOf(".")));
            }

        } catch (IOException ex) {
            Log.d(TAG, ex.toString());
        }
    }

    /**
     * 根据插件文件的输入流与插件名 安装插件
     *
     * @param stream
     * @param pluginName
     * @return
     */
    private static int installPlugin(Context appContext, InputStream stream, String pluginName) {
        AppLog.d(TAG, pluginName + "install plugin start .........");
        int retStatus = INSTALL_FAILED_INTERNAL_ERROR;
        byte[] lock = tryLock(pluginName);
        if (PluginManager.isPluginLoad(pluginName)) {
            retStatus = INSTALL_FAILED_HAS_LOAD;
            tryUnLock(pluginName, lock);
            return retStatus;
        }

        clearInstalledPluginFile(appContext, pluginName);

        try {
            String apkfilePath = PluginDirManager.getPluginApkFile(appContext, pluginName);

            FileUtils.copyInputStreamToFile(stream, new File(apkfilePath));

            PackageManager pm = appContext.getPackageManager();
            PackageInfo packageInfo = pm.getPackageArchiveInfo(apkfilePath, PackageManager.GET_SIGNATURES);
            if (packageInfo == null) {
                throw new Exception(FAILED_REASON_APKBAD);
            }

            if (copyNativeLibs(appContext, apkfilePath, pluginName) < 0) {
                throw new Exception(FAILED_REASON_NOT_SUPPORT_ABI);
            }
            retStatus = INSTALL_SUCCEEDED;
            AppLog.d(TAG, pluginName + "install plugin success .........");
        } catch (Exception e) {
            if (e.getMessage().equals(FAILED_REASON_APKBAD)) {
                retStatus = INSTALL_FAILED_INTERNAL_ERROR;
            } else if (e.getMessage().equals(FAILED_REASON_NOT_SUPPORT_ABI)) {
                retStatus = INSTALL_FAILED_NOT_SUPPORT_ABI;
            }

            clearPluginDir(appContext, pluginName);
        } finally {
            tryUnLock(pluginName, lock);
        }

        return retStatus;

    }

    /**
     * 根据插件文件path与插件名 安装插件
     *
     * @param filePath
     * @param pluginName
     * @return
     */
    private static int installPlugin(Context appContext, String filePath, String pluginName) {
        int retStatus = INSTALL_FAILED_INTERNAL_ERROR;

        byte[] lock = tryLock(pluginName);
        if (PluginManager.isPluginLoad(pluginName)) {
            retStatus = INSTALL_FAILED_HAS_LOAD;
            tryUnLock(pluginName, lock);
            return retStatus;
        }

        try {
            PackageManager pm = appContext.getPackageManager();
            PackageInfo packageInfo = pm.getPackageArchiveInfo(filePath, PackageManager.GET_SIGNATURES);
            if (packageInfo == null) {
                throw new Exception(FAILED_REASON_APKBAD);
            }

            clearInstalledPluginFile(appContext, pluginName);

            if (copyNativeLibs(appContext, filePath, pluginName) < 0) {
                throw new Exception(FAILED_REASON_NOT_SUPPORT_ABI);
            }

            String apkfilePath = PluginDirManager.getPluginApkFile(appContext, pluginName);
            File file = new File(filePath);
            File apkFile = new File(apkfilePath);
            if (file.getParent().equals(apkFile.getParent())) {
                file.renameTo(apkFile);
            } else {
                FileUtils.copyFile(file, apkFile);
            }
            retStatus = INSTALL_SUCCEEDED;

        } catch (Exception e) {
            if (e.getMessage().equals(FAILED_REASON_APKBAD)) {
                retStatus = INSTALL_FAILED_INTERNAL_ERROR;
            } else if (e.getMessage().equals(FAILED_REASON_NOT_SUPPORT_ABI)) {
                retStatus = INSTALL_FAILED_NOT_SUPPORT_ABI;
            }

            clearPluginDir(appContext, pluginName);
        } finally {
            tryUnLock(pluginName, lock);
        }

        return retStatus;
    }

    /**
     * 根据插件文件 安装插件
     *
     * @param appContext
     * @param file
     * @return
     */
    static boolean installUpdate(Context appContext, File file) {
        boolean ret = false;
        PluginUpdateInfoList.PluginUpdateInfo updateApkInfo = parseInfoFromFileName(file.getName());
        if (updateApkInfo == null) {
            FileUtils.deleteQuietly(file);
            // TODO: 2016/8/12 update文件名错误
            return ret;
        }

        if (!isPluginPermitted(appContext, file)) {// TODO 插件文件不合法
            AppLog.e(TAG, file.getAbsolutePath() + " 不合法，安装失败");
            FileUtils.deleteQuietly(file);
            return ret;
        } else {
            AppLog.e(TAG, file.getAbsolutePath() + "合法，校验成功");
        }

        if (PluginManager.isPluginLoad(updateApkInfo.getPluginName())) {
            AppLog.d(TAG,updateApkInfo.getPluginName()+"已载入内存，新版本被禁止安装");
            return ret;
        }
        if (isPluginInstalled(appContext, updateApkInfo.getPluginName())) {
            PackageInfo installedPackageInfo = getInstalledPackageInfo(appContext, updateApkInfo.getPluginName(),
                    PackageManager.GET_ACTIVITIES);
            if (installedPackageInfo == null) {
                FileUtils.deleteQuietly(file);
                // TODO: 2016/8/12 已安装的插件信息获取失败
                return ret;
            }
            if (installedPackageInfo.versionCode >= updateApkInfo.getVersionCode()) {
                FileUtils.deleteQuietly(file);
                return ret;
            }
        }

        int retCode = installPlugin(appContext, file.getAbsolutePath(), updateApkInfo
                .getPluginName());
        switch (retCode) {
            case INSTALL_FAILED_HAS_LOAD:
                break;
            case INSTALL_SUCCEEDED:
                ret = true;
                break;
            case INSTALL_FAILED_INTERNAL_ERROR:
            case INSTALL_FAILED_NOT_SUPPORT_ABI:
            default:
                FileUtils.deleteQuietly(file);
                break;
        }
        return ret;
    }

    /**
     * 清理已安装插件文件
     *
     * @param pluginName
     */
    private static void clearInstalledPluginFile(Context appContext, String pluginName) {
        FileUtils.deleteQuietly(new File(PluginDirManager.getPluginApkFile(appContext, pluginName)));
        FileUtils.deleteQuietly(new File(PluginDirManager.getPluginDalvikCacheFile(appContext, pluginName)));
        FileUtils.deleteQuietly(new File(PluginDirManager.getPluginNativeLibraryDir(appContext, pluginName)));
    }

    /**
     * 清理插件目录
     * @param appContext
     * @param pluginName
     */
    private static void clearPluginDir(Context appContext, String pluginName){
        FileUtils.deleteQuietly(new File(PluginDirManager.getPluginDir(appContext, pluginName)));
    }

    /**
     * 卸载插件
     *
     * @param pluginName
     * @return
     */
    static boolean unInstall(Context appContext, String pluginName) {
        byte[] lock = tryLock(pluginName);

        clearInstalledPluginFile(appContext, pluginName);

        tryUnLock(pluginName, lock);
        return true;

    }

    /**
     * 判断插件是否安装
     *
     * @param pluginName
     * @return
     */
    static boolean isPluginInstalled(Context appContext, String pluginName) {
        return PluginDirManager.isPluginApkFileExist(appContext, pluginName);
    }

    /**
     * 根据插件名获得apk包信息PackageInfo
     *
     * @param appContext
     * @param pluginName
     * @return
     */
    public static PackageInfo getInstalledPackageInfo(Context appContext, String pluginName, int flag) {
        if (!isPluginInstalled(appContext, pluginName)) {
            return null;
        }

        PackageManager pm = appContext.getPackageManager();
        if (pm != null) {
            String apkfilePath = PluginDirManager.getPluginApkFile(appContext, pluginName);
            if (!TextUtils.isEmpty(apkfilePath)) {
                return pm.getPackageArchiveInfo(apkfilePath, flag);
            }
        }
        return null;
    }

    /**
     * copy native so 到相应的插件安装目录
     *
     * @param appContext
     * @param apkfile
     * @param pluginName
     * @return
     * @throws Exception
     */
    private static int copyNativeLibs(Context appContext, String apkfile, String pluginName) throws Exception {
        String nativeLibraryDir = PluginDirManager.getPluginNativeLibraryDir(appContext, pluginName);
        return NativeLibraryHelperCompat.copyNativeBinaries(new File(apkfile), new File(nativeLibraryDir));
    }

    /**
     * 删除插件原文件
     *
     * @param appContext
     * @param pluginApk
     */
    static void deleteAllVerOfPlugin(Context appContext, File pluginApk) {
        PluginUpdateInfoList.PluginUpdateInfo infoPluginApk = parseInfoFromFileName(pluginApk.getName());
        if (infoPluginApk == null) {
            return;
        }
        String pluginName = infoPluginApk.getPluginName();
        File[] apks = getPluginsForUpdate(appContext, pluginName);
        for (File file : apks) {
            if (!FileUtils.deleteQuietly(file)) {
                // TODO: 2016/8/12 删除失败
            }
        }
    }

    /**
     * 根据插件基本信息，生成插件文件名。
     * 对应方法 parseInfoFromFileName
     *
     * @param info
     * @return
     */
    static String generateFileNameByPluginInfoFromNet(PluginUpdateInfoList.PluginUpdateInfo info) {
        if (info == null) {
            return null;
        }
        return info.getPluginName() + "-" + info.getVersionCode() + "-" + info.isOnlyWifi() + ".apk";
    }

    /**
     * 根据插件基本信息，生成下载插件文件名。
     * 对应方法 parseInfoFromFileName
     *
     * @param info
     * @return
     */
    static String generateFileNameByPluginInfoToDownload(PluginUpdateInfoList.PluginUpdateInfo info) {
        if (info == null) {
            return null;
        }
        return info.getPluginName() + "-" + info.getVersionCode() + "-" + info.isOnlyWifi() + ".apk";
    }

    /**
     * 根据插件文件名称获得插件基本信息
     * 对应方法 generateFileNameByPluginInfoFromNet
     *
     * @param fileName
     * @return
     */
    static PluginUpdateInfoList.PluginUpdateInfo parseInfoFromFileName(String fileName) {
        if (!fileName.startsWith(PREFIX_FOR_UPDATE)) {
            return null;
        }
        fileName = fileName.substring(PREFIX_FOR_UPDATE.length());
        PluginUpdateInfoList.PluginUpdateInfo pluginInfo = null;
        try {
            String fileNameWithoutExtensionName = fileName.substring(0, fileName.lastIndexOf("."));
            String[] infos = fileNameWithoutExtensionName.split("-");
            if (infos == null || infos.length < 3) {
                return null;
            }

            pluginInfo = new PluginUpdateInfoList.PluginUpdateInfo();
            pluginInfo.setPluginName(infos[0]);
            pluginInfo.setVersionCode(Integer.parseInt(infos[1]));
            pluginInfo.setOnlyWifi(Boolean.getBoolean(infos[2]));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return pluginInfo;
    }

    static File getLastVerPluginForUpdate(Context context, String pluginName) {
        File[] filesForUpdate = getPluginsForUpdate(context, pluginName);
        return getLastVerPluginForUpdate(filesForUpdate);
    }

    /**
     * 根据插件名称获得最新版本插件apk文件
     *
     * @param files
     * @return
     */
    private static File getLastVerPluginForUpdate(File[] files) {
        File lastVerPlugin = null;
        if (files.length > 0) {
            lastVerPlugin = files[0];
        }
        for (File file : files) {
            PluginUpdateInfoList.PluginUpdateInfo infoInUpdate = parseInfoFromFileName(file.getName());
            PluginUpdateInfoList.PluginUpdateInfo infoLastVer = parseInfoFromFileName(lastVerPlugin.getName());
            if (infoInUpdate == null) {
                continue;
            }
            if (infoLastVer == null) {
                lastVerPlugin = file;
                continue;
            }

            if (infoInUpdate.getVersionCode() > infoLastVer.getVersionCode()) {
                lastVerPlugin = file;
            }
        }
        return lastVerPlugin;
    }

    /**
     * 获得需要更新的插件文件集合
     *
     * @return
     */
    private static List<File> getPluginsForUpdate(Context appContext) {
        List<File> ret = new LinkedList<>();
        String baseDir = PluginDirManager.getBaseDir(appContext);
        if (!TextUtils.isEmpty(baseDir)) {
            File filesInBaseDir = new File(baseDir);
            File[] files = filesInBaseDir.listFiles();
            for (File file : files) {
                File lastVerPluginForUpdate = getLastVerPluginForUpdate(appContext, file.getName());
                if (lastVerPluginForUpdate != null) {
                    ret.add(lastVerPluginForUpdate);
                }
            }
        }
        return ret;
    }

    /**
     * 根据插件名获得未安装插件的文件
     * @param context
     * @param pluginName
     * @return
     */
    private static File[] getPluginsForUpdate(Context context, String pluginName) {
        String pluginDir = PluginDirManager.getPluginApkDir(context, pluginName);
        File[] retFiles = new File(pluginDir).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if (filename.startsWith(PREFIX_FOR_UPDATE) && filename.endsWith(".apk")) {
                    return true;
                }
                return false;
            }
        });
        return retFiles;
    }

    /**
     * 校验插件文件合法性
     *
     * @param appContext
     * @param apkFile
     * @return
     */
    private static boolean isPluginPermitted(Context appContext, File apkFile) {
        if (!apkFile.exists() || !apkFile.isFile()) {
            return false;
        }

        try {
            PackageInfo pluginPackageInfo = appContext.getPackageManager().getPackageArchiveInfo(apkFile
                    .getAbsolutePath(), PackageManager.GET_META_DATA);
            PackageInfo mainPackageInfo = appContext.getPackageManager().getPackageInfo(appContext.getPackageName(),
                    PackageManager.GET_SIGNATURES);
            if (null == pluginPackageInfo || null == mainPackageInfo) {
                return false;
            }
            ApplicationInfo pluginApplicationInfo = pluginPackageInfo.applicationInfo;
            if (null == pluginApplicationInfo) {
                return false;
            }
            int minVersionOfMainApp = pluginApplicationInfo.metaData.getInt("hostMinVer", 0);
            //主程序版本小于插件定义的最小主程序版本，或插件未定义最小主程序版本，校验不通过
            if (minVersionOfMainApp <= 0 || minVersionOfMainApp > mainPackageInfo.versionCode) {
                return false;
            }

            //主程序是release包时校验插件publicKey与主程序的一致性
            return PluginInitializer.isValidPublicKey() ? publicKeyIsValid(appContext, mainPackageInfo, apkFile) : true;
        } catch (Exception e) {
            // TODO: 2016/8/12 错误报告
        }
        return false;
    }

    /**
     * 校验公共key
     *
     * @param appContext
     * @param mainPackageInfo
     * @param apkFile
     * @return
     * @throws Exception
     */
    private static boolean publicKeyIsValid(Context appContext, PackageInfo mainPackageInfo, File apkFile) throws
            Exception {
        PackageInfo pluginPackageInfo = appContext.getPackageManager().getPackageArchiveInfo(apkFile.getAbsolutePath
                (), PackageManager.GET_SIGNATURES);
        if (pluginPackageInfo == null) {
            return false;
        }
        Signature[] pluginSigns = pluginPackageInfo.signatures;
        if (pluginSigns == null || pluginSigns.length == 0) {
            return false;
        }
        Signature pluginSign = pluginSigns[0];
        if (pluginSign == null) {
            return false;
        }
        String pluginKey = parsePublicKey(pluginSign.toByteArray());

        Signature[] signs = mainPackageInfo.signatures;
        if (signs == null || signs.length == 0) {
            return false;
        }
        Signature sign = signs[0];
        if (sign == null) {
            return false;
        }
        String hostKey = parsePublicKey(sign.toByteArray());
        AppLog.e(TAG, "hostKey=" + hostKey);
        AppLog.e(TAG, "pluginKey=" + pluginKey);

        return hostKey != null && hostKey.equals(pluginKey);
    }

    /**
     * format公共key，将二进制数组转为string
     *
     * @param signature
     * @return
     */
    public static String parsePublicKey(byte[] signature) {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream
                    (signature));
            String pubKey = cert.getPublicKey().toString();
            return pubKey;
        } catch (CertificateException e) {
            e.printStackTrace();
            // TODO: 2016/8/12 报告异常
        }
        return null;
    }

    public static byte[] tryLock(String pluginName) {
        byte[] lockObj = new byte[0];
        byte[] lock = mLockMap.get(pluginName);
        if (lock == null) {
            mLockMap.put(pluginName, lockObj);
        } else {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return lock;
    }

    public static void tryUnLock(String pluginName, byte[] lock) {
        if (lock != null) {
            synchronized (lock){
                lock.notify();
            }
        } else {
            mLockMap.remove(pluginName);
        }
    }
}
