package com.fragplugin.base;

import android.content.Context;

import com.fragplugin.base.update.IPluginUpdateRequest;
import com.fragplugin.base.utils.MultiThreadAsyncTask;


/**
 * Created by lixiaoyu on 2016/8/9.
 */
public class PluginInitializer {
    private static boolean LOAD_LOCAL = false;

    private static boolean VALID_PUBLIC_KEY = false;

    public static boolean isLoadLocal() {
        return LOAD_LOCAL;
    }

    public static boolean isValidPublicKey() {
        return VALID_PUBLIC_KEY;
    }

    public static void init(Context context, IPluginUpdateRequest pluginUpdateRequest, boolean loadLocal, boolean validPublicKey) {
        LOAD_LOCAL = loadLocal;
        VALID_PUBLIC_KEY = validPublicKey;
        PluginUpdateManager.init(pluginUpdateRequest);
        new InitTask(context.getApplicationContext()).execute();
    }

    private static class InitTask extends MultiThreadAsyncTask<Void, Void, Void> {
        private Context mContext;

        InitTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (LOAD_LOCAL) {
                PluginInstallHandler.installDebugPlugins(mContext);
            }else {
                PluginInstallHandler.installUpdate(mContext);
                PluginUpdateManager.checkUpdatePlugins(mContext,null);
            }
            return super.doInBackground(params);
        }
    }
}