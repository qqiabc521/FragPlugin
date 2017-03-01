package com.ljj.fragplugin.plugin1;

import android.content.Context;

import com.fragplugin.base.core.PluginBase;
import com.ljj.fragplugin.plugin1.ui.Plugin1FirstFragment;
import com.ljj.fragplugin.plugin1.ui.Plugin1SecondFragment;

/**
 * Created by Lijj on 17/2/9.
 */

public class LauncherPlugin extends PluginBase {

    public static final String PLUGIN_NAME = PLUGIN_PLUGIN1;

    private static String[][] mConfigs = {
            {Plugin1FirstFragment.class.getSimpleName(), Plugin1FirstFragment.class.getName()},
            {Plugin1SecondFragment.class.getSimpleName(), Plugin1SecondFragment.class.getName()}
    };

    public LauncherPlugin(Context hostContext) {
        super(hostContext);
    }


    @Override
    public String[][] getConfigPages() {
        return mConfigs;
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }
}
