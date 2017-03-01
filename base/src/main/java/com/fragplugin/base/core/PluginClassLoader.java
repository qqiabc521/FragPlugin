package com.fragplugin.base.core;

import dalvik.system.DexClassLoader;

/**
 * Created by lqp on 16-2-29.
 */
public class PluginClassLoader extends DexClassLoader {
    private ClassLoader mHostLoader = null;

    public final String nativeLibraryDir;

    public PluginClassLoader(String dexPath,
                             String optimizedDirectory,
                             String libraryPath, ClassLoader parent) {

        super(dexPath, optimizedDirectory, libraryPath, parent);

        mHostLoader = parent;
        nativeLibraryDir = libraryPath;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        //Log.e("pluginLoader", "***findClass: " + name);
        return super.findClass(name);
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        //Log.e("pluginLoader", "===loadClass: " + className);
        return super.loadClass(className, resolve);
    }
}
