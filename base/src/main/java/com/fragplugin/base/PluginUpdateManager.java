package com.fragplugin.base;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.fragplugin.base.exception.PluginVersionBelowException;
import com.fragplugin.base.update.IPluginUpdateRequest;
import com.fragplugin.base.update.PluginStatusObserve;
import com.fragplugin.base.update.PluginUpdateInfoList;
import com.fragplugin.base.update.PluginUpdateInfoListener;
import com.fragplugin.base.utils.AppLog;
import com.fragplugin.base.utils.NetWorkUtils;
import com.fragplugin.base.utils.StreamUtils;
import com.fragplugin.base.utils.WenbaThreadPool;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PluginUpdateManager {
    private static final String TAG = PluginUpdateManager.class.getSimpleName();

    private static IPluginUpdateRequest mPluginUpdateRequest;

    private static final Map<String, Integer> mPluginVerMap = Collections.synchronizedMap(new HashMap<String, Integer>());

    private static Map<String, ArrayList<PluginStatusObserve>> pluginStatusObserves = Collections.synchronizedMap(new
            HashMap<String, ArrayList<PluginStatusObserve>>());

    private static PluginUpdateInfoList pluginUpdateInfoList = null;

    private static String availablePluginsConfig;

    public static void init(IPluginUpdateRequest pluginUpdateRequest) {
        mPluginUpdateRequest = pluginUpdateRequest;
    }

    public static void clear() {
        pluginUpdateInfoList = null;
        pluginStatusObserves.clear();
    }

    /**
     * 根据插件名获得插件更新基本信息
     *
     * @param pluginName
     * @param infos
     * @return
     */
    private static PluginUpdateInfoList.PluginUpdateInfo getDownloadAnablePluginInfo(String pluginName,
                                                                              List<PluginUpdateInfoList
                                                                                      .PluginUpdateInfo> infos) {
        if (infos == null) {
            return null;
        }
        for (PluginUpdateInfoList.PluginUpdateInfo info : infos) {
            if (pluginName != null && info != null) {
                if (pluginName.equals(info.getPluginName())) {
                    return info;
                }
            }
        }
        return null;
    }

    /**
     * 下载插件文件
     *
     * @param info
     */
    private static void downloadPlugin(Context hostContext, PluginUpdateInfoList.PluginUpdateInfo info) {
        String fileName = PluginInstallHandler.generateFileNameByPluginInfoToDownload(info);
        String downloadPath = PluginDirManager.getPluginApkDir(hostContext, info.getPluginName()) + "/" + fileName;
        AppLog.e(TAG, "插件下载：" + info.getPluginName() + " ,downloadPath=" + downloadPath);

//        WenbaDownLoader.download(info.getPluginUrl(), downloadPath, true , new PluginsDownLoadCallBack(hostContext, info));

    }

    private static void loadPluginOnFaile(String pluginName, String msg) {
        if (pluginName != null && pluginStatusObserves.containsKey(pluginName)) {
            ArrayList<PluginStatusObserve> observes = pluginStatusObserves.get(pluginName);
            Iterator<PluginStatusObserve> iterator = observes.iterator();
            while (iterator.hasNext()) {
                PluginStatusObserve observe = iterator.next();
                if (observe != null) {
                    observe.onFaile(msg);
                }
            }
        }
    }


    private static String getString(Context hostContext, int id) {
        return hostContext.getString(id);
    }

    /**
     * 检查更新插件
     *
     * @param pluginName
     */
    public static void checkUpdatePlugins(final Context hostContext, final String pluginName) {

        if (pluginUpdateInfoList == null || pluginUpdateInfoList.getList() == null || pluginUpdateInfoList.getList()
                .isEmpty()) {
            if (mPluginUpdateRequest == null) {
                WenbaThreadPool.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadPluginOnFaile(pluginName,"初始化错误，重启应用");
                    }
                });
                return;
            }

            String requestParam = getAvailablePluginsConfig(hostContext);
            mPluginUpdateRequest.startRequest(requestParam, new PluginUpdateInfoListener() {

                @Override
                public void onSuccess(PluginUpdateInfoList list) {
                    if (list == null || list.getList() == null || list.getList().isEmpty()) {
                        loadPluginOnFaile(pluginName, getString(hostContext, R.string.plugin_faile_server_config_notfound));
                    } else {
                        pluginUpdateInfoList = list;

                        checkUpdatePlugins(hostContext,pluginName, list.getList());
                    }
                }

                @Override
                public void onFaile(int code, String msg) {
                    loadPluginOnFaile(pluginName, msg);
                }
            });
        } else {
            checkUpdatePlugins(hostContext,pluginName, pluginUpdateInfoList.getList());
        }
    }

    public static void updatePluginAsyn(Context hostContext, String pluginName, PluginStatusObserve observe) {
        ArrayList<PluginStatusObserve> observeList = null;
        if (pluginStatusObserves.containsKey(pluginName)) {
            observeList = pluginStatusObserves.get(pluginName);
        }
        if (observeList == null) {
            observeList = new ArrayList<PluginStatusObserve>();
            pluginStatusObserves.put(pluginName, observeList);
        }

        if (!observeList.contains(observe)) {
            observeList.add(observe);
        }

        checkUpdatePlugins(hostContext,pluginName);
    }

    private static boolean isPluginDownloadAnable(Context hostContext, boolean onlyWifi) {
        return onlyWifi ? NetWorkUtils.isWifiActive(hostContext) : true;
    }

    /**
     * 检查更新插件
     *
     * @param pluginName
     * @param pluginInfos
     */
    private static void checkUpdatePlugins(Context hostContext, final String pluginName, List<PluginUpdateInfoList.PluginUpdateInfo> pluginInfos) {
        if (pluginName == null) {
            List<PluginUpdateInfoList.PluginUpdateInfo> needUpdatePlugins = filterNeedUpdatePlugins(hostContext,pluginInfos);

            for (PluginUpdateInfoList.PluginUpdateInfo oneInfo : needUpdatePlugins) {
                if (isPluginDownloadAnable(hostContext,oneInfo.isOnlyWifi())) {
                    downloadPlugin(hostContext,oneInfo);
                }
            }
        } else {
            PluginUpdateInfoList.PluginUpdateInfo pluginInfo = getNeedUpdatePlugin(hostContext,pluginName, pluginInfos);
            if (pluginInfo != null) {
                downloadPlugin(hostContext,pluginInfo);
                return;
            }
        }
    }

    public static void unregisterPluginStatusObserve(PluginStatusObserve pluginStatusObserve) {
        Set<Map.Entry<String, ArrayList<PluginStatusObserve>>> sets = pluginStatusObserves.entrySet();
        Iterator<Map.Entry<String, ArrayList<PluginStatusObserve>>> entryIterator = sets.iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<String, ArrayList<PluginStatusObserve>> entry = entryIterator.next();
            ArrayList<PluginStatusObserve> observerList = entry.getValue();

            observerList.remove(pluginStatusObserve);

            if (observerList.isEmpty()) {
                entryIterator.remove();
            }
        }
    }

//    private static class PluginsDownLoadCallBack extends WenbaDownloadListener {
//        private PluginUpdateInfoList.PluginUpdateInfo mPluginInfo;
//        private Context mContext;
//
//        PluginsDownLoadCallBack(Context context, PluginUpdateInfoList.PluginUpdateInfo pluginInfo) {
//            mContext = context;
//            mPluginInfo = pluginInfo;
//        }
//
//        @Override
//        public void onDownloadError(String msg) {
//            BBLog.d(TAG,"onFailure download plugin ");
//            loadPluginOnFaile(mPluginInfo.getPluginName(), getString(mContext,R.string.plugin_downlaod_faile));
//        }
//
//        @Override
//        public void onStart() {
//
//        }
//
//        @Override
//        public void onProgress(int progress, long fileCount) {
//            String pluginName = mPluginInfo.getPluginName();
//            if (pluginName != null && pluginStatusObserves.containsKey(pluginName)) {//TODO 当该插件有插件状态更新观察者，更新下载进度
//                ArrayList<PluginStatusObserve> observes = pluginStatusObserves.get(pluginName);
//                Iterator<PluginStatusObserve> iterator = observes.iterator();
//                while (iterator.hasNext()) {
//                    PluginStatusObserve observe = iterator.next();
//                    if (observe != null) {
//                        observe.onDownloading(progress);
//                    }
//                }
//            }
//        }
//
//        @Override
//        public void onFinish(String path) {
//            BBLog.d(TAG,"onSuccess download plugin path = "+ path);
//            File resFile = new File(path);
//            String destPath = resFile.getParent() + "/" + PluginInstallHandler.generateUpdateFileName(resFile.getName());
//            if(!resFile.renameTo(new File(destPath))){
//                destPath = path;
//            }
//            final String installPath = destPath;
//
//            PluginUtil.installPluginPoolExecute(new Runnable() {
//                @Override
//                public void run() {
//                    boolean flag = PluginInstallHandler.installUpdate(mContext, new File(installPath));
//                    BBLog.e(TAG,installPath +" 安装 "+(flag?"成功":"失败"));
//                    if (flag) {
//                        String pluginName = mPluginInfo.getPluginName();
//                        if (pluginName != null && pluginStatusObserves.containsKey(pluginName)) {//TODO
//                            // 当该插件有插件状态更新观察者，更新下载进度
//                            ArrayList<PluginStatusObserve> observes = pluginStatusObserves.get(pluginName);
//                            Iterator<PluginStatusObserve> iterator = observes.iterator();
//                            while (iterator.hasNext()) {
//                                PluginStatusObserve observe = iterator.next();
//                                if (observe != null) {
//                                    PluginManager.loadPlugin(mContext,pluginName, observe);
//                                }
//                            }
//                        }
//                    } else {
//                        WenbaThreadPool.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                loadPluginOnFaile(mPluginInfo.getPluginName(), getString(mContext,R.string.plugin_install_faile));
//                            }
//                        });
//                    }
//                }
//            });
//        }
//
//        @Override
//        public void onCancel() {
//
//        }
//    }

    /**
     * 过滤需要更新的插件信息
     *
     * @param data
     * @return
     */
    static List<PluginUpdateInfoList.PluginUpdateInfo> filterNeedUpdatePlugins(Context hostContext, List<PluginUpdateInfoList.PluginUpdateInfo>
                                                                                data) {
        List<PluginUpdateInfoList.PluginUpdateInfo> retData = new ArrayList<>();
        for (final PluginUpdateInfoList.PluginUpdateInfo oneInfo : data) {
            //TODO 根据插件名获得该插件的已安装信息
            try {
                if (checkPluginUpdate(hostContext,oneInfo)) {
                    retData.add(oneInfo);
                }
            } catch (PluginVersionBelowException e) {
                e.printStackTrace();
                WenbaThreadPool.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadPluginOnFaile(oneInfo.getPluginName(), "插件不可用，版本过低");
                    }
                });
            }
        }

        Iterator<PluginUpdateInfoList.PluginUpdateInfo> iterator = retData.iterator();
        while (iterator.hasNext()) {
            PluginUpdateInfoList.PluginUpdateInfo pluginInfo = iterator.next();
            if (checkPluginDownloaded(hostContext,pluginInfo)) {
                iterator.remove();
            }
        }

        return retData;
    }

    /**
     * 根据插件名，获取该插件需要更新的信息
     *
     * @param pluginName
     * @param data
     * @return
     */
    private static PluginUpdateInfoList.PluginUpdateInfo getNeedUpdatePlugin(Context hostContext, final String pluginName, List<PluginUpdateInfoList
            .PluginUpdateInfo> data) {
        PluginUpdateInfoList.PluginUpdateInfo pluginInfo = getDownloadAnablePluginInfo(pluginName, data);
        if (pluginInfo == null) {
            WenbaThreadPool.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadPluginOnFaile(pluginName, pluginName + " 后台无相关信息配置");
                }
            });
            return null;
        }
        try {
            if (checkPluginUpdate(hostContext,pluginInfo)) {
                if (!checkPluginDownloaded(hostContext,pluginInfo)) {
                    return pluginInfo;
                }
            }
        } catch (PluginVersionBelowException e) {
            e.printStackTrace();
            WenbaThreadPool.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loadPluginOnFaile(pluginName, "插件不可用，版本过低");
                }
            });

        }
        return null;
    }

    /**
     * 根据插件最新配置信息，检查该插件是否需要给更新安装
     *
     * @param pluginInfo
     * @return
     */
    private static boolean checkPluginUpdate(Context hostContext, PluginUpdateInfoList.PluginUpdateInfo pluginInfo) throws
            PluginVersionBelowException {
        if (pluginInfo == null) {
            return false;
        }

        int minAvalibleVersion = getAvailablePluginMinVersion(hostContext,pluginInfo.getPluginName());
        if (pluginInfo.getVersionCode() < minAvalibleVersion) {
            throw new PluginVersionBelowException(pluginInfo.getPluginName() + "version is " + pluginInfo
                    .getVersionCode() + " below app limit minAvalibleVersion = " + minAvalibleVersion);
        }

        PackageInfo info = PluginInstallHandler.getInstalledPackageInfo(hostContext, pluginInfo.getPluginName(),
                PackageManager.GET_ACTIVITIES);
        if (info == null) {
            return true;
        }
        if (pluginInfo.getVersionCode() > info.versionCode) {
            return true;
        }
        return false;
    }

    /**
     * 根据插件最新配置信息，判断该插件是否已经下载
     *
     * @param pluginInfo
     * @return
     */
    private static boolean checkPluginDownloaded(Context hostContext, PluginUpdateInfoList.PluginUpdateInfo pluginInfo) {
        File lastVerPlugin = PluginInstallHandler.getLastVerPluginForUpdate(hostContext, pluginInfo.getPluginName());
        if (lastVerPlugin != null) {
            String fileName = PluginInstallHandler.generateUpdateFileName(PluginInstallHandler.generateFileNameByPluginInfoFromNet(pluginInfo));
            if (lastVerPlugin.getName().equals(fileName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获得可用插件的最小版本
     *
     * @param pluginName
     * @return
     */
    static int getAvailablePluginMinVersion(Context hostContext, String pluginName) {
        if (mPluginVerMap.isEmpty()) {
            formatAvailablePluginVersions(hostContext,mPluginVerMap);
        }
        if (mPluginVerMap.containsKey(pluginName)) {
            return mPluginVerMap.get(pluginName);
        }
        return 1;
    }

    private static final synchronized void formatAvailablePluginVersions(Context hostContext, Map<String, Integer> pluginVerMap) {
        if (!pluginVerMap.isEmpty()) {
            return;
        }
        String config = getAvailablePluginsConfig(hostContext);
        if(config == null){
            return;
        }
        JSONArray jsonObjs = null;
        try {
            jsonObjs = new JSONArray(config);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (jsonObjs != null) {
            for (int i = 0; i < jsonObjs.length(); i++) {
                try {
                    JSONObject jsonObj = (JSONObject) jsonObjs.get(i);
                    String name = jsonObj.getString("name");
                    int pluginMinVer = jsonObj.getInt("minver");
                    pluginVerMap.put(name, pluginMinVer);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String getAvailablePluginsConfig(Context hostContext) {
        if (availablePluginsConfig == null) {
            availablePluginsConfig = readAvailablePluginsConfig(hostContext);
        }
        return availablePluginsConfig;
    }

    private static synchronized String readAvailablePluginsConfig(Context hostContext) {
        try {
            InputStream inStream = hostContext.getAssets().open("plugin_ver_map.json");
            byte[] jsonStr = StreamUtils.readFromStreamToByte(inStream);

            if (jsonStr != null) {
                JSONArray jsonArray = new JSONObject(new String(jsonStr)).getJSONArray("plugins");
                return jsonArray.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
