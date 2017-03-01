package com.fragplugin.base.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * @author xunwang
 * 
 *         2015-5-13
 */
public class NetWorkUtils {

	public static final int NETWORK_TYPE_WIFI = ConnectivityManager.TYPE_WIFI;
	public static final int NETWORK_TYPE_MOBILE = ConnectivityManager.TYPE_MOBILE;
	public static final int NETWORK_TYPE_NONETWORK = -1;
	
	/**
	 * wifi网络是否可用
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isWifiActive(Context context) {
		if (context == null) {
			return false;
		}
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ninfo = cm.getActiveNetworkInfo();
		return ninfo != null && ninfo.isAvailable() && ninfo.getType() == ConnectivityManager.TYPE_WIFI && ninfo.getDetailedState() == DetailedState.CONNECTED;
	}

	/**
	 * 移动网络是否可用
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isMobleActive(Context context) {
		if (context == null) {
			return false;
		}
		
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ninfo = cm.getActiveNetworkInfo();
		return ninfo != null && ninfo.isAvailable() && ninfo.getType() == ConnectivityManager.TYPE_MOBILE && ninfo.getDetailedState() == DetailedState.CONNECTED;
	}

	/**
	 * 网络是否可用
	 * @param context
	 * @return
	 */
	public static boolean checkNetWork(Context context) {
		if (context != null) {
			ConnectivityManager mConnectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
			if (mNetworkInfo != null && mNetworkInfo.isConnected()) {
				return (mNetworkInfo.getState() == NetworkInfo.State.CONNECTED) && mNetworkInfo.isAvailable();
			}
		}
		return false;
	}

	/**
	 * 获得网络类型
	 * @param context
	 * @return
	 */
	public static int getNetworkInfoType(Context context) {
		ConnectivityManager mConnectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (mConnectivityManager != null) {
			NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
			if (mNetworkInfo != null) {
				if (mNetworkInfo.isAvailable() && mNetworkInfo.isConnected() && (mNetworkInfo.getState() == NetworkInfo.State.CONNECTED)) {
					return mNetworkInfo.getType();
				}
			}
		}
		return NETWORK_TYPE_NONETWORK;
	}

	/**
	 * 获得手机网络ip
	 * 
	 * @param context
	 * @return
	 */
	public static String getNetWorkIP(Context context) {
		int type = getNetworkInfoType(context);
		switch (type) {
		case NETWORK_TYPE_WIFI:
			return getWifiIP(context);
		case NETWORK_TYPE_MOBILE:
			return getMobileIP();
		}
		return "no_network";
	}

	/**
	 * 获得wifi ip
	 * 
	 * @param context
	 * @return
	 */
	public static String getWifiIP(Context context) {
		String ip = null;
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager.isWifiEnabled()) {
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int i = wifiInfo.getIpAddress();
			ip = (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
		}
		return ip;
	}

	/**
	 * 获得手机移动网络ip
	 * 
	 * @return
	 */
	public static String getMobileIP() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException e) {
			AppLog.w("wenba", e);
		}
		return null;
	}
}
