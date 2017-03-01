package com.ljj.fragplugin;

import com.fragplugin.base.CorePageManager;
import com.fragplugin.base.PluginInitializer;
import com.fragplugin.base.update.IPluginUpdateRequest;
import com.fragplugin.base.update.PluginUpdateInfoListener;
import com.ljj.fragplugin.config.CorePageConfig;

public class AppApplication extends com.fragplugin.base.utils.AppApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        CorePageManager.init(this, CorePageConfig.PAGES_MAPPING,false);


        PluginInitializer.init(getApplicationContext(), new PluginUpdateRequest(), true, true);

    }

    @Override
    public boolean isDebug() {
        return true;
    }

    public static class PluginUpdateRequest implements IPluginUpdateRequest {
        @Override
        public void startRequest(String requestParam, PluginUpdateInfoListener pluginUpdateInfoListener) {
            //TODO 这里网络请求，用户检查插件更新
        }
    }

}
