package com.fragplugin.base.utils;

import android.util.Log;

public class AppLog {
	
	public static final String LOG = AppLog.class.getSimpleName();
	
	public static boolean isDebug(){
		return AppApplication.getInstance().isDebug();
	}

	public static void d(String tag, String message) {
		if (!isDebug() || message == null)
			return;
		Log.d(tag==null?LOG:tag, message);
	}

	public static void e(String tag, String message) {
		if (!isDebug() || message == null)
			return;
		Log.e(tag==null?LOG:tag, message);
	}

	public static void w(String tag, String message) {
		if (!isDebug() || message == null)
			return;
		Log.w(tag==null?LOG:tag, message);
	}

	public static void i(String tag, String message) {
		if (!isDebug() || message == null)
			return;
		Log.i(tag==null?LOG:tag, message);
	}

	public static void wtf(String tag, String message) {
		if (!isDebug() || message == null)
			return;
		Log.wtf(tag==null?LOG:tag, message);
	}
	
	public static void w(String tag, Throwable th) {
		if (!isDebug() )
			return;
		Log.w(tag==null?LOG:tag, "exception",th);
	}

}
