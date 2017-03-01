package com.fragplugin.base.update;

/**
 * Created by Lijj on 16/8/26.
 */
public interface PluginUpdateInfoListener {
    public void onSuccess(PluginUpdateInfoList list);

    public void onFaile(int code, String msg);
}
