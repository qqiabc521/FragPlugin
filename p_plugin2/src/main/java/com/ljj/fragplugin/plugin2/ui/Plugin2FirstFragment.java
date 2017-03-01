package com.ljj.fragplugin.plugin2.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fragplugin.base.corepage.core.CoreAnim;
import com.ljj.fragplugin.appcomm.PluginFragment;
import com.ljj.fragplugin.appcomm.ResManager;
import com.ljj.fragplugin.plugin2.LauncherPlugin;
import com.ljj.fragplugin.plugin2.R;

/**
 * Created by Lijj on 17/2/9.
 *
 * PluginFragment，默认使用Host的Resource
 * 如果在插件中需要使用插件的资源，需要使用getPluginResources()获取插件的Resource
 *
 */

public class Plugin2FirstFragment extends PluginFragment implements View.OnClickListener{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle
            savedInstanceState) {
        View rootView = getPluginInflater().inflate(R.layout.fragment_first,null);

        TextView titleTextView = (TextView) rootView.findViewById(R.id.plugin2_title);
        TextView appcommTextView = (TextView) rootView.findViewById(R.id.plugin2_appcomm);
        TextView resourceTextView = (TextView) rootView.findViewById(R.id.plugin2_resource);
        rootView.findViewById(R.id.plugin2_nextpage).setOnClickListener(this);

        //TODO 引用插件的资源，需要使用插件的resource
        titleTextView.setText(getPluginResources().getString(R.string.plugin2_string));

        //TODO 引用appcomm的资源，对应资源的Id通过ResManager指定
        appcommTextView.setText(getString(ResManager.string.appcomm_string));

        //TODO 引用resource的资源，在Host与插件都默认打包进入各自的apk
        resourceTextView.setText(getPluginResources().getString(R.string.resource_string));

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    protected String getPluginName() {
        return LauncherPlugin.PLUGIN_NAME;
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.plugin2_nextpage){
            openPage(Plugin2SecondFragment.class.getSimpleName(),null, CoreAnim.slide,true,false);
        }
    }
}
