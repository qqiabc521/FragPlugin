#!/usr/bin/env python
# -*- coding: UTF-8 -*-

'''
Created on Aug 15, 2016

@author: lqp
'''
import helper

configFile = 'batch.prop'

if __name__ == '__main__':
    configs = helper.readPropertyFile(configFile)
    
    plugins = configs['plugins']
    
    pluginArray = plugins.split(',')
    
    pluginArray = map(lambda x : x.strip(), pluginArray)
    
    for item in pluginArray:
        helper.buildLog('start build plugin: ' + item)
        helper.watchExecuteCommand('./build_plugin.py', item, 'PluginBuilder')
    
    helper.buildLog('Successfully build: ' + '[' + ','.join(pluginArray) + ']')
    