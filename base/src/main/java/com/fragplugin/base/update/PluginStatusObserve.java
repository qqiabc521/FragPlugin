package com.fragplugin.base.update;

import android.view.View;

/**
 * Created by Lijj on 16/6/21.
 */
public abstract class PluginStatusObserve {
    private String mPluginName;
    private View view;

    public PluginStatusObserve(String pluginName){
        mPluginName = pluginName;
    }

    public PluginStatusObserve(String pluginName, View view){
        mPluginName = pluginName;
        this.view = view;
    }

    public String getPluginName(){
        return mPluginName;
    }

    public View getView(){
        return view;
    }

    public abstract void onPrepare();

    public abstract void onDownloading(int process);

    public abstract void onSuccess();

    public abstract void onFaile(String msg);

}
