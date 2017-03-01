package com.fragplugin.base;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.fragplugin.base.core.PluginBase;
import com.fragplugin.base.core.PluginClassLoader;
import com.fragplugin.base.core.PluginContext;
import com.fragplugin.base.core.PluginInfo;
import com.fragplugin.base.corepage.core.CorePage;
import com.fragplugin.base.exception.PluginClassNotFoundException;
import com.fragplugin.base.exception.PluginNotFindException;
import com.fragplugin.base.exception.PluginNotLoadException;
import com.fragplugin.base.exception.PluginVersionBelowException;
import com.fragplugin.base.reflect.MethodUtils;
import com.fragplugin.base.update.PluginStatusObserve;
import com.fragplugin.base.utils.AppLog;
import com.fragplugin.base.utils.PluginUtil;
import com.fragplugin.base.utils.WenbaThreadPool;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import dalvik.system.DexClassLoader;

/**
 *插件管理核心类，直接面向上层业务
 */
public class PluginManager {
    private static final String TAG = PluginManager.class.getSimpleName();

    private static final String PLUGIN_LAUNCHER_NAME = "LauncherPlugin";

    private static final Map<String, PluginInfo> mPluginMap = Collections.synchronizedMap(new HashMap<String,
            PluginInfo>());

    private static Map<PluginLoadClassObserve, ClassPluginStatusObserve> classPluginStatusObserves = Collections
            .synchronizedMap(new HashMap<PluginLoadClassObserve, ClassPluginStatusObserve>());

    private static Map<String, ArrayList<PluginStatusObserve>> pluginStatusObserves = Collections.synchronizedMap(new
            HashMap<String,
                    ArrayList<PluginStatusObserve>>());

    /**
     * 获得插件Launcher的PluginBase对象
     *
     * @param pluginContext
     * @param mappingClass
     * @return
     */
    private static PluginBase createPluginLauncher(PluginContext pluginContext, Class<?> mappingClass) throws
            Exception {
        PluginBase pluginBase = (PluginBase) MethodUtils.invokeConstructor(mappingClass, pluginContext);
        return pluginBase;
    }

    /**
     * 根据插件名和fragment全称，获得对应的Fragment对象
     *
     * @param pluginName
     * @param fragmentName
     * @return
     */
    static Fragment getFragment(String pluginName, String fragmentName) {
        Fragment ret = null;
        try {
            Class cls = getClassFromPluginInfo(pluginName, fragmentName);
            ret = (Fragment) MethodUtils.invokeConstructor(cls);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (PluginNotLoadException e) {
            e.printStackTrace();
        } catch (PluginClassNotFoundException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * 根据pluginName获得插件基本信息PluginInfo
     *
     * @param pluginName
     * @return
     */
    public static synchronized PluginInfo getPluginInfo(String pluginName) {
        if (mPluginMap.containsKey(pluginName)) {
            return mPluginMap.get(pluginName);
        }
        return null;
    }

    /**
     * 根据pluginName获得插件的Context
     *
     * @param pluginName
     * @return
     */
    public static PluginContext getPluginContext(String pluginName) throws PluginNotFindException {
        PluginInfo pluginInfo = getPluginInfo(pluginName);
        if (pluginInfo != null) {
            return pluginInfo.context;
        } else {
            throw new PluginNotFindException(pluginName + " not find");
        }
    }

    /**
     * 在已加载的所有plugininfo中查找 Class
     *
     * @param classFullName
     * @return
     * @throws ClassNotFoundException
     */
    static Class<?> getClassInAllPluginInfo(String classFullName) throws ClassNotFoundException {
        Class clazz = null;
        Set<Map.Entry<String, PluginInfo>> entrys = mPluginMap.entrySet();
        Iterator<Map.Entry<String, PluginInfo>> iterator = entrys.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PluginInfo> entry = iterator.next();
            PluginInfo info = entry.getValue();
            try {
                clazz = info.loader.loadClass(classFullName);
            } catch (ClassNotFoundException e) {

            }
        }
        if (clazz == null) {
            throw new ClassNotFoundException(classFullName + "not fund in plugins");
        }

        return clazz;
    }

    /**
     * 根据pluginName 和 classFullName从插件中获得对应的Class
     *
     * @param pluginName
     * @param classFullName
     * @return
     * @throws PluginClassNotFoundException
     * @throws PluginNotLoadException
     * @throws PluginVersionBelowException
     */
    static Class<?> getClassFromPlugin(Context hostContext, String pluginName, String classFullName) throws
            PluginClassNotFoundException, PluginNotLoadException, PluginVersionBelowException {
        if (!isPluginLoad(pluginName)) {
            loadPlugin(hostContext, pluginName);
        }

        PluginInfo info = getPluginInfo(pluginName);
        if (info == null) {
            throw new PluginNotLoadException("plugin = " + pluginName + " not load");
        }

        try {
            return info.loader.loadClass(classFullName);
        } catch (ClassNotFoundException e) {
            throw new PluginClassNotFoundException(classFullName + " not found in " + pluginName);
        }
    }

    /**
     * 根据pluginName和类名全称，获取对应的Class
     *
     * @param pluginName
     * @param classFullName
     * @return
     * @throws PluginClassNotFoundException
     * @throws PluginNotLoadException
     */
    static Class<?> getClassFromPluginInfo(String pluginName, String classFullName) throws
            PluginClassNotFoundException, PluginNotLoadException {
        PluginInfo info = getPluginInfo(pluginName);
        if (info == null) {
            throw new PluginNotLoadException("plugin = " + pluginName + " not load");
        }

        try {
            return info.loader.loadClass(classFullName);
        } catch (ClassNotFoundException e) {
            throw new PluginClassNotFoundException(classFullName + " not found in " + pluginName);
        }
    }

    /**
     * 通过异步回调方式获取插件Class
     *
     * @param pluginName
     * @param classFullName
     * @param callBack
     */
    public static void getPluginClass(Context hostContext, final String pluginName, final String classFullName, final
    PluginLoadClassObserve
            callBack) {

        callBack.loadClassStart();

        if (isPluginLoad(pluginName)) {
            Class<?> foundClass = null;
            try {
                foundClass = getClassFromPluginInfo(pluginName, classFullName);
            } catch (PluginClassNotFoundException e) {
                e.printStackTrace();
            } catch (PluginNotLoadException e) {
                e.printStackTrace();
            }
            if (foundClass != null) {
                callBack.loadClassSuccess(foundClass);
            } else {
                callBack.loadClassFailed(hostContext.getString(R.string.plugin_load_faile_by_name));
            }
            return;
        } else {
            ClassPluginStatusObserve classPluginStatusObserve = new ClassPluginStatusObserve(hostContext,
                    pluginName, classFullName, callBack);
            classPluginStatusObserves.put(callBack, classPluginStatusObserve);

            loadPluginAsync(hostContext, pluginName, classPluginStatusObserve, true);
        }
    }

    /**
     * 检查插件观察者是否在观察列表内
     *
     * @param pluginName
     * @param observe
     * @return
     */
    private static boolean containsObserve(String pluginName, PluginStatusObserve observe) {
        if (pluginName != null && pluginStatusObserves.containsKey(pluginName)) {//TODO 当该插件有插件状态更新观察者，更新下载进度
            ArrayList<PluginStatusObserve> observes = pluginStatusObserves.get(pluginName);
            if (observes == null || observes.isEmpty()) {
                return false;
            }
            return observes.contains(observe);
        }
        return false;
    }

    /**
     * 载入插件
     *
     * @param pluginName
     * @param observe
     */
    static void loadPlugin(final Context hostContext, final String pluginName, final PluginStatusObserve observe) {
        boolean flag = false;
        try {
            flag = loadPlugin(hostContext, pluginName);
        } catch (PluginVersionBelowException e) {
            e.printStackTrace();
        }
        if (flag) {
            WenbaThreadPool.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (observe != null && containsObserve(pluginName, observe)) {
                        observe.onSuccess();
                    }
                }
            });
        } else {
            PluginInstallHandler.unInstall(hostContext, pluginName);
            WenbaThreadPool.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (observe != null && containsObserve(pluginName, observe)) {
                        observe.onFaile(hostContext.getString(R.string.plugin_load_faile_by_name));
                    }
                }
            });
        }
    }

    /**
     * 异步载入插件
     *
     * @param pluginName
     * @param observe
     */
    public static void loadPluginAsync(final Context hostContext, final String pluginName, final PluginStatusObserve
            observe, final boolean needUpdate) {
        ArrayList<PluginStatusObserve> observeList = null;
        if (pluginStatusObserves.containsKey(pluginName)) {
            observeList = pluginStatusObserves.get(pluginName);
        }

        if (observeList == null) {
            observeList = new ArrayList<PluginStatusObserve>();
            pluginStatusObserves.put(pluginName, observeList);
        }

        if (observe != null && !observeList.contains(observe)) {
            observeList.add(observe);
            observe.onPrepare();
        }

        WenbaThreadPool.poolExecute(new Runnable() {
            @Override
            public void run() {
                //TODO 加载插件
                boolean flag = false;
                try {
                    flag = loadPlugin(hostContext, pluginName);
                } catch (PluginVersionBelowException e) {
                    e.printStackTrace();
                    PluginUpdateManager.updatePluginAsyn(hostContext,pluginName, observe);
                    return;
                }
                if (flag) {
                    //TODO 加载成功
                    WenbaThreadPool.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (observe != null && containsObserve(pluginName, observe)) {
                                observe.onSuccess();
                            }
                        }
                    });
                    return;
                } else {//TODO 加载失败
                    //TODO 插件是否安装
                    if (PluginInstallHandler.isPluginInstalled(hostContext, pluginName)) {
                        loadPlugin(hostContext, pluginName, observe);
                        return;
                    }

                    //TODO 获取最新为安装插件文件
                    final File pluginApk = PluginInstallHandler.getLastVerPluginForUpdate(hostContext, pluginName);
                    if (pluginApk != null && pluginApk.exists()) {
                        PluginUtil.installPluginPoolExecute(new Runnable() {
                            @Override
                            public void run() {
                                if (PluginInstallHandler.installUpdate(hostContext, pluginApk)) {
                                    if (containsObserve(pluginName, observe)) {
                                        loadPlugin(hostContext, pluginName, observe);
                                    }
                                } else {
                                    WenbaThreadPool.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (observe != null && containsObserve(pluginName, observe)) {
                                                AppLog.d("jason", "mHostContext");
                                                observe.onFaile(hostContext.getString(R.string.plugin_install_faile));
                                            }
                                        }
                                    });
                                }
                            }
                        });
                        return;
                    }
                    if (needUpdate && !PluginInitializer.isLoadLocal()) {
                        PluginUpdateManager.updatePluginAsyn(hostContext,pluginName, observe);
                    } else {
                        WenbaThreadPool.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (observe != null && containsObserve(pluginName, observe)) {
                                    observe.onFaile(hostContext.getString(R.string.plugin_not_fund_by_pluginfile));
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * 判断插件是否已加载
     *
     * @param pluginName
     * @return
     */
    public static boolean isPluginLoad(String pluginName) {
        return mPluginMap.containsKey(pluginName);
    }

    /**
     * 注销PluginLoadClassObserve
     *
     * @param pluginLoadClassObserve
     */
    public static void unregisterPluginLoadClassObserve(PluginLoadClassObserve pluginLoadClassObserve) {
        if (pluginLoadClassObserve == null) {
            return;
        }
        if (classPluginStatusObserves.containsKey(pluginLoadClassObserve)) {
            PluginStatusObserve pluginStatusObserve = classPluginStatusObserves.get(pluginLoadClassObserve);
            if (pluginStatusObserve != null) {
                unregisterPluginStatusObserve(pluginStatusObserve);
            }
            classPluginStatusObserves.remove(pluginLoadClassObserve);
        }
    }

    /**
     * 注销PluginStatusObserve
     *
     * @param pluginStatusObserve
     */
    public static void unregisterPluginStatusObserve(PluginStatusObserve pluginStatusObserve) {
        if (pluginStatusObserve == null) {
            return;
        }
        Set<Map.Entry<String, ArrayList<PluginStatusObserve>>> sets = pluginStatusObserves.entrySet();
        Iterator<Map.Entry<String, ArrayList<PluginStatusObserve>>> entryIterator = sets.iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<String, ArrayList<PluginStatusObserve>> entry = entryIterator.next();
            ArrayList<PluginStatusObserve> observerList = entry.getValue();

            if (observerList.contains(pluginStatusObserve)) {
                observerList.remove(pluginStatusObserve);
                PluginUpdateManager.unregisterPluginStatusObserve(pluginStatusObserve);
            }
            if (observerList.isEmpty()) {
                entryIterator.remove();
            }
        }
    }

    /**
     * 创建插件的ClassLoader
     *
     * @param pluginPath
     * @param optDir
     * @param libDir
     * @return
     */
    private static DexClassLoader createDexClassLoader(ClassLoader parentClassLoader, String pluginPath, String
            optDir, String libDir) {
        return new PluginClassLoader(pluginPath, optDir, libDir, parentClassLoader);
    }

    /**
     * 创建AssetManager
     *
     * @param apkPath
     * @return
     */
    private static AssetManager createAssetManager(String apkPath) {
        try {
            AssetManager assetManager = MethodUtils.invokeConstructor(AssetManager.class);
            MethodUtils.invokeMethod(assetManager, "addAssetPath", apkPath);
            return assetManager;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 创建插件Resources
     *
     * @param assetManager
     * @return
     */
    private static Resources createResources(Resources superResource, AssetManager assetManager) {
        Resources resources = new Resources(assetManager, superResource.getDisplayMetrics(), superResource
                .getConfiguration());

        return resources;
    }

    /**
     * 加载插件
     *
     * @param pluginName
     * @return
     */
    private static boolean loadPlugin(Context hostContext, String pluginName) throws PluginVersionBelowException {
        if (isPluginLoad(pluginName)) {
            return true;
        }
        byte[] lock = PluginInstallHandler.tryLock(pluginName);
        boolean ret = false;
        if (!PluginInstallHandler.isPluginInstalled(hostContext, pluginName)) {
            PluginInstallHandler.tryUnLock(pluginName, lock);
            return ret;
        }

        int minAvalibleVersion = PluginUpdateManager.getAvailablePluginMinVersion(hostContext,pluginName);

        if (minAvalibleVersion <= 0) {
            PluginInstallHandler.tryUnLock(pluginName, lock);
            return ret;
        }
        String pluginApk = PluginDirManager.getPluginApkFile(hostContext, pluginName);
        PackageManager pm = hostContext.getPackageManager();
        if (pm == null) {
            PluginInstallHandler.tryUnLock(pluginName, lock);
            return ret;
        }
        PackageInfo packageInfo = pm.getPackageArchiveInfo(pluginApk, PackageManager.GET_ACTIVITIES);
        if (packageInfo.versionCode < minAvalibleVersion) {
            PluginInstallHandler.tryUnLock(pluginName, lock);
            throw new PluginVersionBelowException(pluginName + "version is " + packageInfo.versionCode + " below app " +
                    "limit minAvalibleVersion = " + minAvalibleVersion);
        }

        String optimizedDirectory = PluginDirManager.getPluginDalvikCacheDir(hostContext, pluginName);
        String libraryPath = PluginDirManager.getPluginNativeLibraryDir(hostContext, pluginName);
        try {
            ClassLoader classloader = createDexClassLoader(hostContext.getClassLoader(), pluginApk,
                    optimizedDirectory, libraryPath);

            AssetManager assetManager = createAssetManager(pluginApk);
            Resources resources = createResources(hostContext.getResources(), assetManager);
            Theme theme = createTheme(hostContext, resources);

            PluginContext pluginContext = createPluginContext(hostContext, pluginName, classloader, resources, theme);
            Class<?> pluginLauncherClass = getPluginLauncherClass(pluginContext);

            PluginBase launcher = createPluginLauncher(pluginContext, pluginLauncherClass);

            PluginInfo pluginInfo = new PluginInfo();
            pluginInfo.name = pluginName;
            pluginInfo.libDir = optimizedDirectory;
            pluginInfo.loader = classloader;
            pluginInfo.resources = resources;
            pluginInfo.theme = theme;
            pluginInfo.context = pluginContext;
            pluginInfo.pluginBase = launcher;

            mPluginMap.put(pluginName, pluginInfo);
            ret = true;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        } finally {
            PluginInstallHandler.tryUnLock(pluginName, lock);
        }
        return ret;
    }

    /**
     * 通过debug模式载入插件信息，即把自身作为插件载入
     *
     * @param pluginName
     * @return
     */
    public static boolean loadPluginByDebug(Context hostContext, String pluginName) {
        try {
            ClassLoader classloader = hostContext.getClassLoader();

            Resources resources = hostContext.getResources();
            Theme theme = hostContext.getTheme();

            PluginContext pluginContext = createPluginContext(hostContext, pluginName, classloader, resources, theme);
            Class<?> pluginLauncherClass = getPluginLauncherClass(pluginContext);

            PluginBase launcher = createPluginLauncher(pluginContext, pluginLauncherClass);

            PluginInfo pluginInfo = new PluginInfo();
            pluginInfo.name = pluginName;
            pluginInfo.loader = classloader;
            pluginInfo.resources = resources;
            pluginInfo.theme = theme;
            pluginInfo.context = pluginContext;
            pluginInfo.pluginBase = launcher;

            mPluginMap.put(pluginName, pluginInfo);
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return false;
        }
        return true;
    }

    /**
     * 根据插件名和pageName获得CorePage
     *
     * @param pluginName
     * @param pageName
     * @return
     */
    static CorePage getCorePageInPlugin(String pluginName, String pageName) {
        if (!isPluginLoad(pluginName)) {
            return null;
        }
        return mPluginMap.get(pluginName).pluginBase.getCorePage(pageName);
    }

    private static PluginContext createPluginContext(Context hostContext, String pluginName, ClassLoader classLoader,
                                                     Resources resources, Theme theme) {
        return new PluginContext(hostContext, pluginName, classLoader, resources, theme);
    }

    private static Theme createTheme(Context hostContext, Resources resources) {
        Theme theme = resources.newTheme();
        theme.setTo(hostContext.getTheme());
        return theme;
    }

    /**
     * 获得插件的Launcher入口类
     *
     * @param pluginContext
     * @return
     */
    private static Class<?> getPluginLauncherClass(PluginContext pluginContext) throws ClassNotFoundException {
        String launcherName = new StringBuffer(pluginContext.getPackageName()).append(".").append(PLUGIN_LAUNCHER_NAME).toString();
        return Class.forName(launcherName, false, pluginContext.getClassLoader());
    }

    /**
     * 清理已加载的插件
     */
    public static synchronized void clear() {
        pluginStatusObserves.clear();
        classPluginStatusObserves.clear();

        PluginUpdateManager.clear();
    }

    static class ClassPluginStatusObserve extends PluginStatusObserve {

        private Context mContext;
        private String mClassFullName;
        private PluginLoadClassObserve mCallBack;

        public ClassPluginStatusObserve(Context context, String pluginName, String classFullName,
                                        PluginLoadClassObserve callBack) {
            super(pluginName);
            mContext = context;
            mCallBack = callBack;
            mClassFullName = classFullName;
        }

        @Override
        public void onPrepare() {

        }

        @Override
        public void onDownloading(int process) {
            if (mCallBack != null) {
                mCallBack.loadClassLoading(process);
            }
        }

        @Override
        public void onSuccess() {
            Class<?> foundClass = null;
            try {
                foundClass = PluginManager.getClassFromPluginInfo(getPluginName(),
                        mClassFullName);
            } catch (PluginClassNotFoundException e) {
                e.printStackTrace();
            } catch (PluginNotLoadException e) {
                e.printStackTrace();
            }
            if (foundClass != null) {
                if (mCallBack != null) {
                    mCallBack.loadClassSuccess(foundClass);
                }
            } else {
                if (mCallBack != null) {
                    mCallBack.loadClassFailed(mContext.getString(R.string.plugin_clas_not_found));
                }
            }
        }

        @Override
        public void onFaile(String msg) {
            if (mCallBack != null) {
                mCallBack.loadClassFailed(msg);
            }

        }
    }

}
