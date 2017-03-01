package com.ljj.fragplugin.appcomm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.fragplugin.base.corepage.CorePageFragment;
import com.fragplugin.base.utils.AppLog;
import com.fragplugin.base.utils.AppApplication;

public abstract class BaseFragment extends CorePageFragment {
    private static final String TAG = BaseFragment.class.getSimpleName();

    private boolean isDestroyed = false;
    

    public BaseFragment() {
        super();
        AppLog.d(TAG, this + " instance");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        AppLog.d(TAG, this + " onAttach");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLog.d(TAG, this + " onCreate");
        setRetainInstance(false);
        // 如果savedInstanceState不为Null说明是页面是销毁后重建的，不过统计（这点待确认）
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        AppLog.d(TAG, this + " onDestroyView");
        isDestroyed = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        AppLog.d(TAG, this + " onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        AppLog.d(TAG, this + " onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        AppLog.d(TAG, this + " onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        AppLog.d(TAG, this + " onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppLog.d(TAG, this + " onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        AppLog.d(TAG, this + " onDetach");
    }

    protected Context getApplicationContext() {
        if (getActivity() != null) {
            return getActivity().getApplicationContext();
        } else {
            return AppApplication.getInstance().getApplicationContext();
        }
    }

    public boolean isPageDestroyed() {
        return isDestroyed;
    }

    public void sendBroadcast(Intent intent) {
        LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(intent);
    }

    public Intent registerReceiver(BroadcastReceiver receiver,
                                   IntentFilter filter) {
        if (isDestroyed) {
            return null;
        }
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(receiver, filter);
        return null;
    }

    public Intent registerReceiver(BroadcastReceiver receiver,
                                   IntentFilter filter, boolean system) {
        if (system) {
            return getActivity().registerReceiver(receiver, filter);
        }
        return registerReceiver(receiver, filter);
    }

    public void unregisterReceiver(BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(getApplicationContext())
                .unregisterReceiver(receiver);
    }

    public void unregisterReceiver(BroadcastReceiver receiver, boolean system) {
        if (system) {
            getActivity().unregisterReceiver(receiver);
        } else {
            LocalBroadcastManager.getInstance(getApplicationContext())
                    .unregisterReceiver(receiver);
        }
    }


}
