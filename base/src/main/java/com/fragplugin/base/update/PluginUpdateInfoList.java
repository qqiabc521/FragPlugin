package com.fragplugin.base.update;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Lijj on 16/6/21.
 */
public class PluginUpdateInfoList implements Serializable {
    private List<PluginUpdateInfo> list;
    public List<PluginUpdateInfo> getList() {
        return list;
    }
    public void setList(List<PluginUpdateInfo> list) {
        this.list = list;
    }

    public PluginUpdateInfoList(){
    }

    public static class PluginUpdateInfo implements Serializable {
        private String pluginName;
        private int versionCode;
        private String pluginUrl;
        private boolean onlyWifi;

        public String getPluginName() {
            return pluginName;
        }

        public void setPluginName(String pluginName) {
            this.pluginName = pluginName;
        }

        public int getVersionCode() {
            return versionCode;
        }

        public void setVersionCode(int versionCode) {
            this.versionCode = versionCode;
        }

        public String getPluginUrl() {
            return pluginUrl;
        }

        public void setPluginUrl(String pluginUrl) {
            this.pluginUrl = pluginUrl;
        }

        public boolean isOnlyWifi() {
            return onlyWifi;
        }

        public void setOnlyWifi(boolean onlyWifi) {
            this.onlyWifi = onlyWifi;
        }
    }
}
