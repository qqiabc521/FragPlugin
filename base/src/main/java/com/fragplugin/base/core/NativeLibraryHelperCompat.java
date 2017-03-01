package com.fragplugin.base.core;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import com.fragplugin.base.reflect.MethodUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NativeLibraryHelperCompat {

    private static final String TAG = NativeLibraryHelperCompat.class.getSimpleName();

    private static final Class nativeLibraryHelperClass() throws ClassNotFoundException {
        return Class.forName("com.android.internal.content.NativeLibraryHelper");
    }

    private static final Class handleClass() throws ClassNotFoundException {
        return Class.forName("com.android.internal.content.NativeLibraryHelper$Handle");
    }

    public static final int copyNativeBinaries(File apkFile, File sharedLibraryDir) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return copyNativeBinariesAfterL(apkFile, sharedLibraryDir);
        } else {
            return copyNativeBinariesBeforeL(apkFile, sharedLibraryDir);
        }

    }

    private static int copyNativeBinariesBeforeL(File apkFile, File sharedLibraryDir) {
        try {
            Object[] args = new Object[2];
            args[0] = apkFile;
            args[1] = sharedLibraryDir;
            return (int) MethodUtils.invokeStaticMethod(nativeLibraryHelperClass(), "copyNativeBinariesIfNeededLI", args);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return -1;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static int copyNativeBinariesAfterL(File apkFile, File sharedLibraryDir) {
        try {
            Object handleInstance = MethodUtils.invokeStaticMethod(handleClass(), "create", apkFile);
            if (handleInstance == null) {
                return -1;
            }

            String abi = null;

            if (isVM64(apkFile.getAbsolutePath())) {
                if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
                    Set<String> abis = getAbisFromApk(apkFile.getAbsolutePath());
                    if (abis == null || abis.isEmpty()) {
                        return 0;
                    }
                    int abiIndex = (int) MethodUtils.invokeStaticMethod(nativeLibraryHelperClass(), "findSupportedAbi", handleInstance, Build.SUPPORTED_64_BIT_ABIS);
                    if (abiIndex >= 0) {
                        abi = Build.SUPPORTED_64_BIT_ABIS[abiIndex];
                    }
                }
            } else {
                if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
                    Set<String> abis = getAbisFromApk(apkFile.getAbsolutePath());
                    if (abis == null || abis.isEmpty()) {
                        return 0;
                    }
                    int abiIndex = (int) MethodUtils.invokeStaticMethod(nativeLibraryHelperClass(), "findSupportedAbi", handleInstance, Build.SUPPORTED_32_BIT_ABIS);
                    if (abiIndex >= 0) {
                        abi = Build.SUPPORTED_32_BIT_ABIS[abiIndex];
                    }
                }
            }

            if (abi == null) {
                return -1;
            }

            Object[] args = new Object[3];
            args[0] = handleInstance;
            args[1] = sharedLibraryDir;
            args[2] = abi;
            return (int) MethodUtils.invokeStaticMethod(nativeLibraryHelperClass(), "copyNativeBinaries", args);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return -1;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean isVM64(String apk) {
        Set<String> supportedAbis = getAbisFromApk(apk);
        if (Build.SUPPORTED_64_BIT_ABIS.length == 0) {
            return false;
        }

        if (supportedAbis == null || supportedAbis.isEmpty()) {
            return true;
        }

        for (String supportedAbi : supportedAbis) {
            if ("arm64-v8a".endsWith(supportedAbi) || "x86_64".equals(supportedAbi) || "mips64".equals(supportedAbi)) {
                return true;
            }
        }

        return false;
    }

    private static Set<String> getAbisFromApk(String apk) {
        ZipFile apkFile = null;
        try {
            apkFile = new ZipFile(apk);
            Enumeration<? extends ZipEntry> entries = apkFile.entries();
            Set<String> supportedAbis = new HashSet<>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.contains("../")) {
                    continue;
                }
                if (name.startsWith("lib/") && !entry.isDirectory() && name.endsWith(".so")) {
                    String supportedAbi = name.substring(name.indexOf("/") + 1, name.lastIndexOf("/"));
                    supportedAbis.add(supportedAbi);
                }
            }
            return supportedAbis;
        } catch (Exception e) {
            Log.e(TAG, "get supportedAbis failure", e);
        }finally {
            if (apkFile != null) {
                try {
                    apkFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }
}
