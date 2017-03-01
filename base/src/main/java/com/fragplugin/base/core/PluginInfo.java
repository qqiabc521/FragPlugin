package com.fragplugin.base.core;

import android.content.res.Resources;
import android.content.res.Resources.Theme;

public class PluginInfo{
	public String name;
	public String libDir;
	public ClassLoader loader;
	public Resources resources;
	public Theme theme;
	public PluginContext context;
	public PluginBase pluginBase;
}
