package com.fragplugin.base.core;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.view.LayoutInflater;

import com.fragplugin.base.PluginDirManager;

import java.io.File;

/**
 *
 * 该类是插件访问资源的接口类,充当常规开发中Context到角色,但是对以下行为做了修改:
 * 1. getResources(), getAssets(),访问的是插件apk里的资源
 * 2. getFilesDir(), 该目录对应于插件的文件目录
 * 3. getSharedPreferences(), 获取到对应插件的SharedPreferences
 * 4. getDatabasePath(), 获取数据库文件的路径,结合PluginDatabaseOpenHelper使用
 *
 * 以上做法到目的在于在于资源空间到管理,每个插件通过PluginContext访问到的是属于自己局部空间的
 * 资源,避免了全局空间下名称冲突的问题,便于管理
 *
 */
public final class PluginContext extends ContextWrapper {
    public static final String PLUGIN_PREFIX = "p_";

    private String mPluginName;
    private Resources mPluginResource;
    private Resources.Theme mPluginThem;
    private ClassLoader mClassLoader;

    public PluginContext(Context base, String pluginName, ClassLoader pluginClassLoader, Resources pluginResource, Resources.Theme pluginThem) {
        super(base);
        mPluginName = pluginName;
        mPluginResource = pluginResource;
        mPluginThem = pluginThem;
        mClassLoader = pluginClassLoader;
    }

    @Override
    public String getPackageName() {
        if(mClassLoader instanceof PluginClassLoader){
            return new StringBuffer(super.getPackageName()).append(".").append(mPluginName.substring(PLUGIN_PREFIX.length()))
                    .toString();
        }
        return super.getPackageName();
    }

    /**
     * 获取插件的 SharedPreferences
     * @param name
     * @return
     */

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return  super.getSharedPreferences(getNamePrefix() + name, mode);
    }

    private String getNamePrefix() {
        return new StringBuffer().append("prefs_plugin_").append(mPluginName).append("_").toString();
    }

    @Override
    public File getDatabasePath(String name) {
        File dir = new File(PluginDirManager.getPluginDataDir(this,name));
        File dbFile = new File(dir, name);
        return dbFile;
    }

    /**
     * 获取插件的数据文件目录
     * @return
     */
    @Override
    public File getFilesDir() {
        return new File(PluginDirManager.getPluginFilesDir(this,mPluginName));
    }

    @Override
    public Resources getResources() {
        return mPluginResource;
    }

    @Override
    public AssetManager getAssets() {
        return mPluginResource.getAssets();
    }

    @Override
    public Resources.Theme getTheme() {
        return mPluginThem;
    }

    @Override
    public Object getSystemService(String name) {
        if (!Context.LAYOUT_INFLATER_SERVICE.equals(name)) {
            return super.getSystemService(name);
        }

        LayoutInflater inflater = (LayoutInflater) super.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LayoutInflater proxyInflater = inflater.cloneInContext(this);

        return proxyInflater;
    }

    @Override
    public ClassLoader getClassLoader() {
        return mClassLoader;
    }
}
