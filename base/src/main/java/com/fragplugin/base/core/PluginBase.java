package com.fragplugin.base.core;

import android.content.Context;

import com.fragplugin.base.corepage.core.CorePage;
import com.fragplugin.base.utils.AppLog;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public abstract class PluginBase {
    public static final String PLUGIN_PLUGIN1 = "p_plugin1";
    public static final String PLUGIN_PLUGIN2 = "p_plugin2";

    private Context mContext;


    private Map<String, CorePage> mPageMap = new HashMap<String, CorePage>();

    protected abstract String[][] getConfigPages();

    protected abstract String getPluginName();

    public PluginBase(Context context) {
        mContext = context;
        readBaseConfig(getConfigPages());
    }


    private static boolean isFileInAssets(Context context, String fileName) {
        try {
            String[] listName = context.getAssets().list("");
            for (int i = 0; i < listName.length; i++) {
                if (fileName.endsWith(listName[i])) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static byte[] getFromAssets(Context context, String fileName) {
        if (context == null || fileName == null) {
            return null;
        }

        if (!isFileInAssets(context, fileName)) {
            return null;
        }

        byte[] dataBuffer = null;
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(fileName);
            dataBuffer = new byte[inputStream.available()];

            inputStream.read(dataBuffer);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return dataBuffer;
    }

    private void readBaseConfig(String[][] config) {
        if (config != null && config.length > 0) {
            for (int i = 0; i < config.length; i++) {
                CorePage corePage = new CorePage(config[i][0], config[i][1], null, getPluginName());
                AppLog.d("plugin", corePage.toString());
                mPageMap.put(config[i][0], corePage);
            }
        }
    }

    public CorePage getCorePage(String pageName) {
        if (mPageMap.containsKey(pageName)) {
            return mPageMap.get(pageName);
        }
        return null;
    }

}
