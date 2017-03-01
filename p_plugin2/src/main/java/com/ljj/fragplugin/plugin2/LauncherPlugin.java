package com.ljj.fragplugin.plugin2;

import android.content.Context;

import com.fragplugin.base.core.PluginBase;
import com.ljj.fragplugin.plugin2.ui.Plugin2FirstFragment;
import com.ljj.fragplugin.plugin2.ui.Plugin2SecondFragment;

/**
 * Created by Lijj on 17/2/9.
 */

public class LauncherPlugin extends PluginBase {

    public static final String PLUGIN_NAME = PLUGIN_PLUGIN2;

    private static String[][] mConfigs = {
            {Plugin2FirstFragment.class.getSimpleName(), Plugin2FirstFragment.class.getName()},
            {Plugin2SecondFragment.class.getSimpleName(), Plugin2SecondFragment.class.getName()}
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
