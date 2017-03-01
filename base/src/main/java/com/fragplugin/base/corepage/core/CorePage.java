/**
 * Copyright 2015 ZhangQu Li
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fragplugin.base.corepage.core;

import com.fragplugin.base.core.PluginBase;
import com.fragplugin.base.core.PluginContext;

import java.io.Serializable;

/**
 * assets/page.json 页面属性类
 */
public class CorePage implements Serializable {
    private static final long serialVersionUID = 3736359137726536495L;

    private static String[][] plugins = null;

    static{
        plugins = new String[][]{
                {PluginBase.PLUGIN_PLUGIN1.substring(PluginContext.PLUGIN_PREFIX.length()), PluginBase.PLUGIN_PLUGIN1},
                {PluginBase.PLUGIN_PLUGIN2.substring(PluginContext.PLUGIN_PREFIX.length()), PluginBase.PLUGIN_PLUGIN2}
        };
    }

    //页面名
    private String mName;

    //页面class
    private String mClazz;

    //传入参数，json object结构
    private String mParams;

    //父模块
    private String mSupers;

    public CorePage(String name, String clazz, String params, String supers) {
        mName = name;
        mClazz = clazz;
        mParams = params;
        mSupers = supers;
    }

    public String getClazz() {
        return mClazz;
    }

    public void setClazz(String clazz) {
        mClazz = clazz;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getParams() {
        return mParams;
    }

    public void setParams(String params) {
        mParams = params;
    }

    public String getSupers() {
        return mSupers;
    }

    public void setSupers(String supers) {
        this.mSupers = supers;
    }

    public static String getPluginNameByClassName(String className) {
        if(className == null){
            return null;
        }
        String tempName = className.toLowerCase();
        for(int i=0;i<plugins.length;i++){
            if(tempName.startsWith(plugins[i][0])){
                return plugins[i][1];
            }
        }
        return null;
    }

    public static String getPluginNameByPluginPackageName(String packageName) {
        if(packageName == null){
            return null;
        }
        for(int i=0;i<plugins.length;i++){
            if(packageName.startsWith(plugins[i][0])){
                return plugins[i][1];
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Page{" +
                "mSuper='" + mSupers + '\'' +
                ", mName='" + mName + '\'' +
                ", mClazz='" + mClazz + '\'' +
                ", mParams='" + mParams + '\'' +
                '}';
    }
}
