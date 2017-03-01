package com.ljj.fragplugin.plugin1.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ljj.fragplugin.appcomm.PluginFragment;
import com.ljj.fragplugin.plugin1.LauncherPlugin;
import com.ljj.fragplugin.plugin1.R;

/**
 * Created by Lijj on 17/2/9.
 *
 * PluginFragment，默认使用Host的Resource
 * 如果在插件中需要使用插件的资源，需要使用getPluginResources()获取插件的Resource
 *
 */

public class Plugin1SecondFragment extends PluginFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle
            savedInstanceState) {
        return getPluginInflater().inflate(R.layout.fragment_second,null);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    protected String getPluginName() {
        return LauncherPlugin.PLUGIN_NAME;
    }
}
