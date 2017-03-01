package com.fragplugin.base.utils;


import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PluginUtil {
    private static volatile Executor installPluginPool;

    private static final Executor getInstallPluginExecutor() {
        if (installPluginPool != null) {
            return installPluginPool;
        }

        synchronized (PluginUtil.class) {
            if (installPluginPool == null) {
                installPluginPool = Executors.newSingleThreadExecutor();
            }
        }

        return installPluginPool;
    }

    public static void installPluginPoolExecute(Runnable runnable){
        getInstallPluginExecutor().execute(runnable);
    }

}
