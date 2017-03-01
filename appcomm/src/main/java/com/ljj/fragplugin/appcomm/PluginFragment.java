package com.ljj.fragplugin.appcomm;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;

import com.fragplugin.base.exception.PluginNotFindException;
import com.fragplugin.base.PluginManager;


public abstract class PluginFragment extends BaseFragment {
	
	private Context mContext;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			mContext = PluginManager.getPluginContext(getPluginName());
		} catch (PluginNotFindException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Context getPluginContext() {
		return mContext;
	}

	public Resources getPluginResources() {
		return mContext.getResources();
	}

    public LayoutInflater getPluginInflater() {
        return LayoutInflater.from(mContext);
    }

	protected abstract String getPluginName();

}
