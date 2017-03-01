package com.fragplugin.base.utils;

import android.app.Application;

public abstract class AppApplication extends Application {
	protected static AppApplication instance;

	@Override
	public void onCreate() {
		super.onCreate();
        instance = this;
	}

	public static AppApplication getInstance() {
		return instance;
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

	public abstract boolean isDebug();

}
