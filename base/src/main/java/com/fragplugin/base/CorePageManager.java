package com.fragplugin.base;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.fragplugin.base.corepage.CorePageActivity;
import com.fragplugin.base.corepage.CorePageFragment;
import com.fragplugin.base.corepage.FragmentWrap;
import com.fragplugin.base.corepage.core.CorePage;
import com.fragplugin.base.corepage.core.CoreSwitcher;
import com.fragplugin.base.reflect.MethodUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by Lijj on 16/8/3.
 */
public class CorePageManager {
    private static final String TAG = CorePage.class.getSimpleName();

    private static Context mContext;
    private static boolean mPluginDebug = false;

    private static Map<String, CorePage> mPageMap = new HashMap<String, CorePage>();

    public interface OpenPageHandler{
        public void synaLoadPlugin(String pluginName, final String pageName, final Bundle bundle, final int[]
                animations, final boolean addToBackStack, int requestCode, String openPageName);
    }

    public static void init(Context context, String[][] config, boolean pluginDebug){
        mContext = context.getApplicationContext();
        mPluginDebug = pluginDebug;
        readBaseConfig(config);
    }

    private static void readBaseConfig(String[][] config) {
        if(config != null && config.length > 0){
            for(int i=0;i<config.length;i++){
                mPageMap.put(config[i][0], new CorePage(config[i][0],config[i][1],null,null));
            }
        }
    }

    /**
     * 根据pageName获得Fragment
     */
    public static Fragment findFragmentByName(String pageName) {
        CorePageFragment fragment = null;
        try {
            CorePage corePage = mPageMap.get(pageName);
            if (corePage == null) {
                Log.d(TAG, "Page:" + pageName + " is null");
                return null;
            }

            fragment = (CorePageFragment) Class.forName(corePage.getClazz()).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fragment;
    }

    /**
     * 页面跳转核心函数之一
     * 打开一个fragemnt
     *
     * @param fragmentManager FragmentManager管理类
     * @param pageName  页面名
     * @param bundle 参数
     * @param animations 动画类型
     * @param addToBackStack 是否添加到返回栈
     * @return 打开的Fragment对象
     */
    public static FragmentWrap openPageWithNewFragmentManager(FragmentManager fragmentManager, String pageName, Bundle bundle, int[] animations, boolean addToBackStack, int requestCode, OpenPageHandler openPageHandler, String openPageName) {
        FragmentWrap ret = new FragmentWrap();
        CorePageFragment fragment = null;
        try {
            CorePage corePage = mPageMap.get(pageName);
            if (corePage == null) {
                String pluginName = getPluginNameByClassName(pageName);
                if(pluginName == null) {
                    ret.setFragment(null);
                    return ret;
                }

                if(!PluginManager.isPluginLoad(pluginName)){
                    if(mPluginDebug){
                        PluginManager.loadPluginByDebug(mContext,pluginName);
                    }else {
                        if (openPageHandler != null) {
                            openPageHandler.synaLoadPlugin(pluginName, pageName, bundle, animations, addToBackStack, requestCode, openPageName);

                            ret.setStatus(FragmentWrap.LOADING);
                            return ret;
                        }
                        ret.setFragment(null);
                        return ret;
                    }
                }

                corePage = PluginManager.getCorePageInPlugin(pluginName,pageName);
                if(corePage == null){
                    ret.setFragment(null);
                    return ret;
                }
                fragment = (CorePageFragment)PluginManager.getFragment(pluginName,corePage.getClazz());
            }else{
                fragment = (CorePageFragment) MethodUtils.invokeConstructor(Class.forName(corePage.getClazz()));
            }

            /**
             * Atlas的支持 end
             */

            Bundle pageBundle = buildBundle(corePage);
            if (bundle != null) {
                bundle.setClassLoader(CorePageActivity.class.getClassLoader());
                pageBundle.putAll(bundle);
            }
            fragment.setArguments(pageBundle);
            fragment.setPageName(pageName);

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            if (animations != null && animations.length >= 4) {
                fragmentTransaction.setCustomAnimations(animations[0], animations[1], animations[2], animations[3]);
            }
            Fragment fragmentContainer = fragmentManager.findFragmentById(R.id.fragment_container);
            if (fragmentContainer != null) {
                fragmentTransaction.hide(fragmentContainer);
            }

            fragmentTransaction.add(R.id.fragment_container, fragment, pageName);
            if (addToBackStack) {
                fragmentTransaction.addToBackStack(pageName);
                Log.d(TAG, "Fragment addToBackStack:" + pageName);
            }

            fragmentTransaction.commitAllowingStateLoss();
            ret.setFragment(fragment);

            Log.d(TAG, "fragmentManager stack count :" + fragmentManager.getBackStackEntryCount());

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Fragment.error:" + e.getMessage());
            ret.setFragment(null);
        }
        return ret;
    }

    /**
     * 根据page，从pageParams中获得bundle
     *
     * @param corePage 页面
     * @return 页面的参数
     */
    private static Bundle buildBundle(CorePage corePage) {
        Bundle bundle = new Bundle(CorePageActivity.class.getClassLoader());
        String key = null;
        Object value = null;
        if (corePage != null && corePage.getParams() != null) {
            com.alibaba.fastjson.JSONObject j = JSON.parseObject(corePage.getParams());
            if (j != null) {
                Set<String> keySet = j.keySet();
                if (keySet != null) {
                    Iterator<String> ite = keySet.iterator();
                    while (ite.hasNext()) {
                        key = ite.next();
                        value = j.get(key);
                        bundle.putString(key, value.toString());
                    }
                }
            }
        }
        return bundle;
    }

    /**
     * 判断fragment是否位于栈顶
     *
     * @param context
     *            上下文
     * @param fragmentTag
     *            fragment的tag
     * @return 是否是栈顶Fragment
     */
    public static boolean isFragmentTop(Context context, String fragmentTag) {
        if (context != null && context instanceof CoreSwitcher) {
            return ((CoreSwitcher) context).isFragmentTop(fragmentTag);
        } else {
            CorePageActivity topActivity = CorePageActivity.getTopActivity();
            if (topActivity != null) {
                return topActivity.isFragmentTop(fragmentTag);
            } else {
                return false;
            }
        }
    }

    /**
     * 根据className获得对应的pluginName
     * @param className
     * @return
     */
    private static String getPluginNameByClassName(String className){
        if(className == null){
            return className;
        }
        String appPackageName = mContext.getPackageName();
        if(className.startsWith(appPackageName)){
            className = className.substring(appPackageName.length()+1);
            String packageName = className.substring(0,className.indexOf("."));
            return CorePage.getPluginNameByPluginPackageName(packageName);
        }else{
            return CorePage.getPluginNameByClassName(className);
        }

    }

    /**
     * 根据className从插件中获得Class
     * @param className
     * @return
     * @throws ClassNotFoundException
     */
    public static Class<?> loadPluginClass(String className) throws ClassNotFoundException {
        String pluginName = getPluginNameByClassName(className);
        if(pluginName != null){
            try {
                return PluginManager.getClassFromPlugin(mContext,pluginName,className);
            } catch (Exception e) {
                throw new ClassNotFoundException("Class = "+className +" not found in "+pluginName);
            }
        }else{
            return PluginManager.getClassInAllPluginInfo(className);
        }

    }
}
