/*
**        DroidPlugin Project
**
** Copyright(c) 2015 Andy Zhang <zhangyong232@gmail.com>
**
** This file is part of DroidPlugin.
**
** DroidPlugin is free software: you can redistribute it and/or
** modify it under the terms of the GNU Lesser General Public
** License as published by the Free Software Foundation, either
** version 3 of the License, or (at your option) any later version.
**
** DroidPlugin is distributed in the hope that it will be useful,
** but WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
** Lesser General Public License for more details.
**
** You should have received a copy of the GNU Lesser General Public
** License along with DroidPlugin.  If not, see <http://www.gnu.org/licenses/lgpl.txt>
**
**/

package com.fragplugin.base;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 插件目录结构
 * 基本目录： 宿主/Plugins
 * 单个插件的基本目录： 基础目录/Plugin ApplicationId
 * source_dir： 单个插件的基础目录/apk/base-1.apk
 * dex缓存目录：单个插件的基础目录/dalvik-cache/
 * lib目录：    单个插件的基础目录/lib/
 * data目录：   单个插件的基础目录/data/
 * 签名文件路径 单个插件的基础目录/Signature/
 */
public class PluginDirManager {


    private static String BASE_DIR = null;

    private static void init(Context context) {
        if (BASE_DIR == null) {
            File baseDir = new File(context.getCacheDir().getParentFile(), "plugins");
            BASE_DIR = enforceDirExists(baseDir);
        }
    }

    private static String enforceDirExists(File file) {
        if (!file.exists()) {
            file.mkdirs();
        }
        return file.getPath();
    }

    private static String makePluginBaseDir(Context context, String pluginName) {
        init(context);
        return enforceDirExists(new File(BASE_DIR, pluginName));
    }

    public static String getBaseDir(Context context) {
        init(context);
        return BASE_DIR;
    }

    public static String getPluginDataDir(Context context, String pluginName) {
        return enforceDirExists(new File(makePluginBaseDir(context, pluginName), "data/" + pluginName));
    }

    public static String getPluginSignatureDir(Context context, String pluginName) {
        return enforceDirExists(new File(makePluginBaseDir(context, pluginName), "Signature/"));
    }

    public static String getPluginSignatureFile(Context context, String pluginName, int index) {
        return new File(getPluginSignatureDir(context, pluginName), String.format("Signature_%s.key", index)).getPath();
    }

    public static List<String> getPluginSignatureFiles(Context context, String pluginName) {
        ArrayList<String> files = new ArrayList<String>();
        String dir = getPluginSignatureDir(context, pluginName);
        File d = new File(dir);
        File[] fs = d.listFiles();
        if (fs != null && fs.length > 0) {
            for (File f : fs) {
                files.add(f.getPath());
            }
        }
        return files;
    }

    public static String getPluginDir(Context context, String pluginName){
        return makePluginBaseDir(context, pluginName);
    }

    /**
     * 获得插件apk所在目录
     *
     * @param context
     * @param pluginName
     * @return
     */
    public static String getPluginApkDir(Context context, String pluginName) {
        return enforceDirExists(new File(makePluginBaseDir(context, pluginName), "apk"));
    }

    /**
     * 获得插件apk
     *
     * @param context
     * @param pluginName
     * @return
     */
    public static String getPluginApkFile(Context context, String pluginName) {
        return new File(getPluginApkDir(context, pluginName), "base-1.apk").getPath();
    }

    /**
     * 对应插件目录是否存在插件apk
     *
     * @param context
     * @param pluginName
     * @return
     */
    public static boolean isPluginApkFileExist(Context context, String pluginName) {
        init(context);
        return new File(BASE_DIR + "/" + pluginName + "/apk/base-1.apk").exists();
    }

    public static String getPluginSettingFile(Context context, String pluginName) {
        File file = new File(getPluginFilesDir(context, pluginName), ".settings");
        return file.getAbsolutePath();
    }


    public static String getPluginFilesDir(Context context, String pluginName) {
        File dir = new File(getPluginApkDir(context, pluginName), "files");
        return enforceDirExists(dir);
    }

    public static String getPluginDalvikCacheDir(Context context, String pluginName) {
        return enforceDirExists(new File(makePluginBaseDir(context, pluginName), "dalvik-cache"));
    }

    public static String getPluginNativeLibraryDir(Context context, String pluginName) {
        return enforceDirExists(new File(makePluginBaseDir(context, pluginName), "lib"));
    }


    public static String getPluginDalvikCacheFile(Context context, String pluginName) {
        String dalvikCacheDir = getPluginDalvikCacheDir(context, pluginName);

        String pluginApkFile = getPluginApkFile(context, pluginName);
        String apkName = new File(pluginApkFile).getName();
        String dexName = apkName.replace(File.separator, "@");
        if (dexName.startsWith("@")) {
            dexName = dexName.substring(1);
        }
        return new File(dalvikCacheDir, dexName + "@classes.dex").getPath();
    }

    public static String getContextDataDir(Context context) {
        String dataDir = new File(Environment.getDataDirectory(), "data/").getPath();
        return new File(dataDir, context.getPackageName()).getPath();
    }

    public static void cleanOptimizedDirectory(String optimizedDirectory) {
        try {
            File dir = new File(optimizedDirectory);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null && files.length > 0) {
                    for (File f : files) {
                        f.delete();
                    }
                }
            }

            if (dir.exists() && dir.isFile()) {
                dir.delete();
                dir.mkdirs();
            }
        } catch (Throwable e) {
        }
    }
}
