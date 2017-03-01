package com.fragplugin.base;

/**
 * 插件加载侦听
 */
public interface PluginLoadClassObserve {
    public void loadClassStart();
    public void loadClassLoading(int process);
    public void loadClassSuccess(Class<?> cls);
    public void loadClassFailed(String msg);
}
