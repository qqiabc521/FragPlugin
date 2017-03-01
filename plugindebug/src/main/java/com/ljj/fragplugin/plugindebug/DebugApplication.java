package com.ljj.fragplugin.plugindebug;


import com.fragplugin.base.CorePageManager;
import com.fragplugin.base.utils.AppApplication;

/**
 * Created by Lijj on 16/10/25.
 */

public class DebugApplication extends AppApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        CorePageManager.init(this, null,true);
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    @Override
    public boolean isDebug() {
        return true;
    }


}
