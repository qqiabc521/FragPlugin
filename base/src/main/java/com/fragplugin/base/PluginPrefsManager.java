package com.fragplugin.base;

import android.content.Context;
import android.content.SharedPreferences;

import com.fragplugin.base.exception.PluginNotFindException;

/**
 * 插件SharedPreferences管理基类
 * Created by Lijj on 16/8/8.
 */
public class PluginPrefsManager {

    private static SharedPreferences getSharedPreferences(String pluginName){
        try {
            Context context = PluginManager.getPluginContext(pluginName);
            if(context != null) {
                return context.getSharedPreferences("local", Context.MODE_PRIVATE);
            }
        } catch (PluginNotFindException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static int getInt(String pluginName, String key, int def) {
        SharedPreferences prefs = getSharedPreferences(pluginName);
        return prefs != null ? prefs.getInt(key, def) : -1;
    }

    protected static long getLong(String pluginName, String key, long def) {
        SharedPreferences prefs = getSharedPreferences(pluginName);
        return prefs != null ? prefs.getLong(key, def) : -1;
    }

    protected static String getString(String pluginName, String key, String def) {
        SharedPreferences prefs = getSharedPreferences(pluginName);
        return prefs != null ? prefs.getString(key, def) : null;
    }

    protected static boolean getBoolean(String pluginName, String key, boolean def) {
        SharedPreferences prefs = getSharedPreferences(pluginName);
        return prefs != null ? prefs.getBoolean(key, def) : false;
    }

    protected static float getFloat(String pluginName, String key, float def) {
        SharedPreferences prefs = getSharedPreferences(pluginName);
        return prefs != null ? prefs.getFloat(key, def) : -1;
    }

    protected static void putInt(String pluginName, String key, int value) {
        SharedPreferences prefs = getSharedPreferences(pluginName);
        if(prefs != null) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(key, value);
            editor.apply();
        }
    }

    protected static void putLong(String pluginName, String key, long value) {
        SharedPreferences prefs = getSharedPreferences(pluginName);
        if(prefs != null) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(key, value);
            editor.apply();
        }
    }

    protected static void putString(String pluginName, String key, String value) {
        SharedPreferences prefs = getSharedPreferences(pluginName);
        if(prefs != null) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(key, value);
            editor.apply();
        }
    }

    protected static void putBoolean(String pluginName, String key, boolean value) {
        SharedPreferences prefs = getSharedPreferences(pluginName);
        if(prefs != null) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(key, value);
            editor.apply();
        }
    }

    protected static void putFloat(String pluginName, String key, float value) {
        SharedPreferences prefs = getSharedPreferences(pluginName);
        if(prefs != null) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat(key, value);
            editor.apply();
        }
    }

}
