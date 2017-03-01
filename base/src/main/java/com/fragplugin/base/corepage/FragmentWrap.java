package com.fragplugin.base.corepage;

import android.support.v4.app.Fragment;

/**
 * Created by lixiaoyu on 2016/8/16.
 */
public class FragmentWrap {
    public static final int READY = 0;
    public static final int LOADING = 1;

    private Fragment mFragment;
    private int mStatus;

    public FragmentWrap(){

    }

    public FragmentWrap(Fragment fragment, int status){
        mFragment = fragment;
        mStatus = status;
    }
    public void setFragment(Fragment fragment){
        mFragment = fragment;
    }

    public Fragment getFragment(){
        return mFragment;
    }

    public void setStatus(int status){
        mStatus = status;
    }

    public int getStatus(){
        return mStatus;
    }
}
