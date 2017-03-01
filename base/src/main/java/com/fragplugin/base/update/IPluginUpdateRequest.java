package com.fragplugin.base.update;

/**
 * Created by lixiaoyu on 2016/8/11.
 */

public interface IPluginUpdateRequest {
    public void startRequest(String requestParam, PluginUpdateInfoListener pluginUpdateInfoListener);
}
