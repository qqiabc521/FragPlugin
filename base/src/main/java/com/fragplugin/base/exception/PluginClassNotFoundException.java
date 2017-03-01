package com.fragplugin.base.exception;

/**
 * Created by Lijj on 16/5/19.
 */
public class PluginClassNotFoundException extends Exception {

    public PluginClassNotFoundException(){
        this("plugin class not found");
    }

    public PluginClassNotFoundException(String message){
        super(message);
    }

}
