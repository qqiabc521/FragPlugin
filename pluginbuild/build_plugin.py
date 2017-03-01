#!/usr/bin/env python
# -*- coding: UTF-8 -*-

'''
Created on Feb 22, 2016

@author: lqp

插件编译步骤:
1. 解析插件的build.gradle, 分析依赖,包括 fileTree, project, 以及repo库, 收集依赖的jar包.
2. 打包资源. 生成 R.java, 同时生成资源包,里面打包了drawable资源和assets等[可配置项]
3. 编译. 编译com.wenba.bangbang[可配置项] package下面的class,其他package的class通通忽略; 由于需要依赖其他工程生成的中间文件, 这一步执行了gradle的编译中的:generateDebugResources目标
4. 混淆[可配置项]. 只混淆插件目录下的类,插件依赖的类不混淆(包括自身libs下的jar包和依赖引入的jar包)
5. 打包. dex转换代码, 代码包括插件自身代码和插件自身libs下的jar包. 其他内容分别是jni的so库, jar包下的普通文件的等
6. 签名
7. zipalign

所有公共代码必须放在插件外, 直观来看就是一份代码(包括jar包)只能存在与base或者插件中的一个, 以避免重复造成的不可控

'''

import os
import re
import shutil

import helper

import pom_parser
from pom_parser import RepoItem

logBuildInfo = True
rootDir = '..'
pluginName = ''
pluginPackageName = ''

pluginOutputDir = 'plugin_bin'
genJavaDir = pluginOutputDir + '/gen'
classesDir = pluginOutputDir + '/classes'
repoCacheDir = pluginOutputDir + '/repoCache'
crunchDir = pluginOutputDir + '/res'
assetDir = pluginOutputDir + '/assets'

# assigned while init...
keyStoreFile = None
keyStorePass = None
keyAlias = None
keyAliasPass = None

commResModule = None
doProguard = False
copyPluginToAppAssets = True

onlyAssetInPluginDir = True
onlyCodeInPluginPackage = True
        
resPakcageFile = pluginOutputDir + '/resource.ap_'
apkUnsignedName = pluginOutputDir + '/plugin_' + pluginPackageName + '_unsigned' + '.apk'
apkSignedName = pluginOutputDir + '/plugin_' + pluginPackageName + '.apk'

logFile = None


def buildLog(msg):
    if logFile:
        logFile.write(msg + '\n')
        logFile.flush()
    print(msg)


class PluginBuilder():
    def __init__(self):
        return

    def buildWithGradle(self):
        curDir = os.getcwd()

        os.chdir(rootDir)
        if pluginName == "p_live":
            helper.watchExecuteCommand('./gradlew', ' makeJar')
            helper.watchExecuteCommand('./gradlew', ' mkLiveSo')
        # helper.watchExecuteCommand('./gradlew', 'clean', 'gradle')
        helper.watchExecuteCommand('./gradlew', ':' + pluginName + ':generateDebugResources',
                                   'gradle')
        os.chdir(curDir)

    def makeOutputDir(self):
        if os.path.exists(pluginOutputDir):
            helper.watchExecuteCommand('rm -r', pluginOutputDir)

        helper.watchExecuteCommand('mkdir', pluginOutputDir)
        return

    def gotoPluginDir(self):
        os.chdir(pluginName)
        return

    def parseProjectProperty(self):
        properties = helper.readPropertyFile('local.properties')

        print('project properties: ' + str(properties))

        if properties.has_key('sdk.dir'):
            self.sdkDir = properties['sdk.dir']

        return

    def parseManifestFile(self):
        maniFile = 'src/main/AndroidManifest.xml'

        if not os.path.exists(maniFile):
            helper.error("src/main/AndroidManifest.xml")

        with open(maniFile) as mfd:
            contents = mfd.read()

        found = re.search(r'package="(com\.ljj\.fragplugin\.[^"]+)"', contents)
        if not found:
            helper.error('not package name found in manifest file')

        print('plugin package: ' + found.group(1))

        self.package = found.group(1)
        if os.path.basename(self.package.replace('.', '/')) != pluginPackageName:
            helper.error(
                'The fold name: ' + pluginName + ' NOT equal to plugin name: ' + self.package)

        return

    def parseGradleFile(self, pathPrefix=None, projList=[]):
        global rootDir

        props = {}

        gradleFile = 'build.gradle'
        if pathPrefix:
            gradleFile = rootDir + '/' + pathPrefix + '/' + gradleFile

        # colect java src files

        with open(gradleFile) as bfd:
            lines = bfd.readlines()

        spaceSplitedProps = ('compileSdkVersion',
                             'buildToolsVersion',
                             'minSdkVersion',
                             'targetSdkVersion',
                             'versionCode',
                             'versionName')

        for line in lines:
            line = line.strip()
            if (len(line) == 0):
                continue;

            for propName in spaceSplitedProps:
                if propName in line:
                    props[propName] = line.split(' ')[1]

        if not pathPrefix:
            self.target = props['compileSdkVersion']
            self.tools = helper.stripQuotedString(props['buildToolsVersion'])

        # parse dependency
        # extract depends block by regex

        content = ' '.join(lines)

        depBlock = re.search(r'dependencies\s*{([^}]+)}', content, re.MULTILINE)
        if not depBlock:
            return props

        # extract compile lines
        depString = depBlock.group(1)
        compGroups = re.findall(r'^\s*(compile.+$)', depString, re.MULTILINE)

        depDict = {}

        depDict['repo'] = []
        depDict['jars'] = []
        depDict['projs'] = []

        for item in compGroups:
            # extract repository reference, project reference, default project libs dir
            valLine = item.replace('compile', '').strip()

            if valLine.startswith('\'') or valLine.startswith('"'): #依赖jar包
                val = helper.strUntil(valLine[1:], valLine[0])
                depDict['repo'].append(val);
                # print val

            elif valLine.startswith('project('): #依赖module
                val = helper.strUntil(valLine[len('project('):], ')')
                val = helper.stripProjectString(val)
                if val in projList:
                    continue

                projList.append(val)
                print(val)

                subProps = self.parseGradleFile(val, projList)  # pluginbase

                subProps['dir'] = val
                depDict['projs'].append(subProps)

                # print val
            elif valLine.startswith('fileTree('):
                val = helper.strUntil(valLine[len('fileTree('):], ')')
                libs = helper.expandJarPath(val, pathPrefix)
                if libs:
                    depDict['jars'] += libs
                    # print(val)

        props['deps'] = depDict

        return props

    def parseBuildProp(self):
        # 1.collect java files to build
        
        includeSrcDir = 'src/main/java/com/ljj/fragplugin/' + pluginPackageName
        if not onlyCodeInPluginPackage:
            includeSrcDir = 'src/main/java'
        
        self.localFiles = helper.collectFilesByPattern(includeSrcDir, \
            r'.*\.java$')

        self.localJars = []
        self.localSoFiles = helper.collectFilesByPattern('src/main/jniLibs', include=r'.*\.so$')
        
        if onlyAssetInPluginDir:
            includeAssetDir = 'src/main/assets/plugin'
        else:
            includeAssetDir = 'src/main/assets'
        
        if os.path.exists(includeAssetDir):
            self.assets = includeAssetDir
        else:
            self.assets = None

        self.depModuleLibs = []
        self.repos = []
        self.repoLibs = []
        self.jars = []

        if not self.props['deps']:
            return

        deps = self.props['deps']

        # 2. depend src folders
        pendItems = []
        pendItems.append(deps)

        opAtPluginRoot = True

        while len(pendItems) > 0:
            head = pendItems.pop()

            if head['repo']:
                if opAtPluginRoot:
                    helper.error('plugin cant refer repo lib, please copy to libs/ folder')

                self.repos += head['repo']

            if head['projs']:
                for projProp in head['projs']:
                    debugLib = rootDir + '/' + projProp[
                        'dir'] + '/build/intermediates/classes/release'
                    self.depModuleLibs.append(debugLib)

                    if 'deps' in projProp:
                        pendItems.append(projProp['deps'])

            if head['jars']:
                if opAtPluginRoot:
                    self.localJars += head['jars']
                else:
                    self.jars += head['jars']

            opAtPluginRoot = False

        helper.deDuplicateStringList(self.repos)
        helper.deDuplicateStringList(self.jars)

        buildLog('localFiles: ' + str(self.localFiles))
        buildLog('localJars: ' + str(self.localJars))
        buildLog('repos: ' + str(self.repos))
        buildLog('depModuleLibs: ' + str(self.depModuleLibs))
        buildLog('jars: ' + str(self.jars))
        return

    def checkBuildEnv(self):
        # android environment
        self.repoDir = self.sdkDir + '/extras/android/m2repository'

        self.androidJar = self.sdkDir + '/platforms/android-' + self.target + '/android.jar'

        buildLog('androidJar path: ' + self.androidJar)

        if not os.path.isfile(self.androidJar):
            helper.error('Target: ' + self.target + ' is not installed')

        # build tools
        buildToolDir = self.sdkDir + '/build-tools/' + self.tools

        if not os.path.exists(buildToolDir):
            helper.error('no build tools find: ' + buildToolDir)

        print('build tools-dir:' + str(buildToolDir))

        self.buildToolDir = buildToolDir
        self.toolsDir = self.sdkDir + '/tools'

        self.aapt = self.buildToolDir + '/aapt'
        self.dx = self.buildToolDir + '/dx'
        self.zipalign = self.buildToolDir + '/zipalign'

        if not os.path.isfile(self.aapt):
            helper.error('aapt is not exist: ' + self.aapt)

        if not os.path.isfile(self.dx):
            helper.error('dx is not exist: ' + self.dx)

        if not os.path.isfile(self.zipalign):
            helper.error('zipalign is not exist: ' + self.aapt)

        helper.watchExecuteCommand('which', 'javac')
        helper.watchExecuteCommand('which', 'jarsigner')
        return

    def doCrunch(self):
        helper.watchExecuteCommand('mkdir', crunchDir)
        helper.watchExecuteCommand(self.aapt + ' crunch', '-v -S src/main/res -C ' + crunchDir,
                                   'crunch png files')
        return

    def copyResNoOverwrite(self, src, dst):
        if not os.path.exists(src):
            helper.error("src folder not exist: " + src)

        if not os.path.exists(dst):
            helper.error("dst folder not exist: " + dst)

        files = os.listdir(src)
        if not files:
            return

        for item in files:
            srcItem = src + '/' + item
            dstItem = dst + '/' + item

            if os.path.isdir(srcItem):
                if not os.path.exists(dstItem):
                    os.mkdir(dstItem)

                buildLog("copy dir: " + srcItem + " to " + dstItem)
                self.copyResNoOverwrite(srcItem, dstItem)

            elif os.path.isfile(srcItem):
                if not os.path.exists(dstItem):
                    helper.watchExecuteCommand('cp', srcItem + ' ' + dstItem)
                else:
                    buildLog("skip exist item: " + dstItem)

        return

    def genApkFile(self):
        self.doCrunch()

        # copy assets
        if self.assets:
            subFolder = ''
            if onlyAssetInPluginDir:
                subFolder = 'plugin'
                
            helper.watchExecuteCommand('mkdir -p', assetDir + '/' + subFolder)
            self.copyResNoOverwrite(self.assets, assetDir + '/' + subFolder, )

        helper.watchExecuteCommand('mkdir', genJavaDir)

        commResOption = ''
        if commResModule:
            commResOption = ' -S ' + rootDir + '/' + commResModule + '/' + 'src/main/res'

        buildArgs = "package" \
                    + ' --no-crunch' \
                    + ' -f -0 apk' \
                    + ' --min-sdk-version ' + self.props['minSdkVersion'] \
                    + ' --target-sdk-version ' + self.props['targetSdkVersion'] \
                    + ' --auto-add-overlay ' \
                    + ' --generate-dependencies' \
                    + ' -G ' + pluginOutputDir + '/proguard.txt' \
                    + commResOption \
                    + " -S " + crunchDir \
                    + ' -S src/main/res' \
                    + " -I " + self.androidJar \
                    + " -M src/main/AndroidManifest.xml"

        if self.assets:
            buildArgs += ' -A ' + assetDir

        buildArgs += " -F " + resPakcageFile + " -m -J " + genJavaDir
        buildArgs += " -P " + genJavaDir + '/pub.xml'
        helper.watchExecuteCommand(self.aapt, buildArgs, 'aapt')
        return

    def combineClassPath(self):
        if hasattr(self, 'classPath'):
            return ':'.join(self.classPath)

        self.classPath = []

        self.extractRepoLibs()

        self.classPath += self.localJars
        self.classPath += self.jars
        self.classPath += self.repoLibs
        self.classPath += self.depModuleLibs

        return ':'.join(self.classPath)

    def buildClass(self):
        helper.watchExecuteCommand('mkdir -p', classesDir, 'mkdirs')

        javaFiles = self.localFiles + helper.collectFilesByPattern(genJavaDir, r'.*\.java$')

        buildArgs = '-g -bootclasspath ' + self.androidJar \
                    + ' -classpath ' + self.combineClassPath() \
                    + ' -source 1.7 -target 1.7' \
                    + ' -d ' + classesDir

        buildArgs += ' ' + ' '.join(javaFiles)

        helper.watchExecuteCommand('javac', buildArgs)
        return

    def proguard(self):
        inJars = pluginOutputDir + '/classes/'
        outJars = pluginOutputDir + '/obfuscated.jar'
        mappingFile = pluginOutputDir + '/mappings.txt'

        cmd = ' -jar ' + self.sdkDir + '/tools/proguard/lib/proguard.jar ' \
              + ' -injars ' + inJars \
              + ' -outjars ' + outJars \
              + ' -printmapping ' + mappingFile \
              + ' @' + self.sdkDir + '/tools/proguard/proguard-android.txt' \
              + ' @proguard-rules.pro' \
              + ' -dontnote ' \
              + ' -keep class com.ljj.fragplugin.' + pluginPackageName + '.LauncherPlugin'

        # library jars

        libJars = ' -libraryjars ' + self.androidJar

        cmd += libJars

        for cp in self.classPath:
            # if cp not in self.localJars:
            cmd += ' -libraryjars ' + cp

        helper.watchExecuteCommand('java', cmd, 'proguard')

        return

    def dexClass(self):
        pluginClassSrc = 'classes/'
        
        if doProguard:
            pluginClassSrc = 'obfuscated.jar'
            self.proguard()
        
        pwd = os.getcwd()
        os.chdir(pluginOutputDir)
        
        dexArgs = '--dex --output=classes.dex %s '%pluginClassSrc
        if len(self.localJars) > 0:
            dexArgs += ' '.join(self.localJars)

        helper.watchExecuteCommand(self.dx, dexArgs, 'dex')

        os.chdir(pwd)
        return

    def doPackage(self):
        classPath = self.toolsDir + '/lib/sdklib.jar'

        buildArgs = ' -cp ' + classPath + ' com.android.sdklib.build.ApkBuilderMain ' \
                    + pluginOutputDir + '/' + pluginName + '_unaligned.apk ' \
                    + ' -u -z ' + resPakcageFile \
                    + ' -f ' + pluginOutputDir + '/classes.dex ' \
                    + ' -rf src/main/java '

        if os.path.exists('libs'):
            buildArgs += ' -rj libs/'

        if os.path.exists('src/main/jniLibs/'):
            buildArgs += ' -nf src/main/jniLibs/'

        helper.watchExecuteCommand('java ', buildArgs, 'apkbuilder')
        return

    def signApk(self):
        pwd = os.getcwd()
        os.chdir(pluginOutputDir)

        unalignedName = pluginName + '_unaligned.apk'

        helper.watchExecuteCommand('jarsigner', \
                                   '-sigalg MD5withRSA -digestalg SHA1 -keystore ' \
                                   + keyStoreFile \
                                   + ' -storepass ' \
                                   + keyStorePass \
                                   + ' -keypass ' + keyAliasPass + ' ' \
                                   + unalignedName + ' ' + keyAlias)

        self.zipalignApk(unalignedName)
        os.chdir(pwd)
        return

    def zipalignApk(self, unalignedName):
        finalName = pluginName + '.apk'
        helper.watchExecuteCommand(self.zipalign, ' 4 ' + unalignedName + ' ' + finalName)
        return

    def extractRepoLibs(self):
        if not self.repos:
            return

        helper.watchExecuteCommand('mkdir', repoCacheDir)

        allNeedRepos = []

        for i in self.repos:
            items = i.split(':')
            if len(items) != 3:
                continue

            pkg = items[0].replace('.', '/')
            name = items[1]
            ver = items[2]

            repItem = RepoItem()
            repItem.groupId = pkg;
            repItem.artifactId = name
            repItem.version = ver

            allNeedRepos += pom_parser.collectRepoAndDeps(repItem, self.repoDir)

        compactRepos = []

        for item in allNeedRepos:
            if item not in compactRepos:
                compactRepos.append(item)

        for item in compactRepos:
            libPath = item.toLibFile(self.repoDir)

            if not os.path.exists(libPath):
                helper.error('repoLib not found: ' + libPath)

            if item.fileType != 'aar':
                # default jars, just copy
                helper.watchExecuteCommand('mkdir -p',
                                           os.path.dirname(repoCacheDir + '/' + item.toLibFile()))
                shutil.copy(libPath, repoCacheDir + '/' + item.toLibFile())
                self.repoLibs.append(repoCacheDir + '/' + item.toLibFile())
                continue

            import zipfile

            zFile = zipfile.ZipFile(libPath, 'r')
            if 'classes.jar' not in zFile.namelist():
                helper.error('Bug: no classes.jar in aar lib')

            itemData = zFile.read('classes.jar')

            dstDirPath = repoCacheDir + '/' + item.toFolder()

            helper.watchExecuteCommand('mkdir -p', dstDirPath)

            dstFilePath = dstDirPath + '/classes.jar'
            dstFile = open(dstFilePath, 'w')

            dstFile.write(itemData)
            dstFile.close()

            self.repoLibs.append(dstFilePath)

            # libs
            if 'libs/' in zFile.namelist():
                for name in zFile.namelist():
                    if name.startswith('libs/') and not name.endswith('/'):
                        dstFilePath = dstDirPath + '/' + os.path.basename(name)
                        # error('zipPath: ' + libPath)
                        # error('dstFilePath: ' + dstFilePath)
                        if os.path.exists(dstFilePath):
                            continue

                        itemData = zFile.read(name)

                        dstFile = open(dstFilePath, 'w')

                        dstFile.write(itemData)
                        dstFile.close()

                        self.repoLibs.append(dstFilePath)

            zFile.close()

        return

    def start(self):
        self.parseProjectProperty()
        self.gotoPluginDir()
        self.props = self.parseGradleFile(projList=[])
        self.parseManifestFile()
        self.parseBuildProp()

        self.checkBuildEnv()
        self.buildWithGradle()
        self.makeOutputDir()

        self.genApkFile()

        self.buildClass()

        self.dexClass()
        self.doPackage()
        self.signApk()
        return

def readGlobalConfig(buildProp):
    cwd = os.getcwd()
    os.chdir(rootDir)
    #read properties
    if 'keyStoreFile' in buildProp:
        global keyStoreFile 
        keyStoreFile = os.path.abspath(buildProp['keyStoreFile'])
    else:
        helper.error('no key store file found')
        
    if 'keyStorePass' in buildProp:
        global keyStorePass 
        keyStorePass = buildProp['keyStorePass']
    else:
        helper.error('no keyStorePass defined')
        
    if 'keyAlias' in buildProp:
        global keyAlias 
        keyAlias = buildProp['keyAlias']
    else:
        helper.error('no keyAlias defined')
        
    if 'keyAliasPass' in buildProp:
        global keyAliasPass
        keyAliasPass = buildProp['keyAliasPass']
    else:
        helper.error('no keyAliasPass defined')
    
    if 'commResModule' in buildProp:
        global commResModule
        commResModule = buildProp['commResModule']
    
    if 'doProguard' in buildProp:
        global doProguard
        if buildProp['doProguard'] == 'true':
            doProguard = True
        else:
            doProguard = False
            
    if 'copyPluginToAppAssets' in buildProp:
        global copyPluginToAppAssets
        if buildProp['copyPluginToAppAssets'] == 'true':
            copyPluginToAppAssets = True
        else:
            copyPluginToAppAssets = False
    
    if 'onlyAssetInPluginDir' in buildProp:
        global onlyAssetInPluginDir
        if buildProp['onlyAssetInPluginDir'] == 'true':
            onlyAssetInPluginDir = True
        else:
            onlyAssetInPluginDir = False
            
    if 'onlyCodeInPluginPackage' in buildProp:
        global onlyCodeInPluginPackage
        if buildProp['onlyCodeInPluginPackage'] == 'true':
            onlyCodeInPluginPackage = True
        else:
            onlyCodeInPluginPackage = False
        
    os.chdir(cwd)
    return

if __name__ == '__main__':
    if len(os.sys.argv) < 2:
        helper.error('Usage: ' + os.sys.argv[0] + ' plugin_name')

    pluginName = os.sys.argv[1]

    helper.logFile = logFile
    # pluginName= 'p_collect'
    if not pluginName.startswith('p_'):
        helper.error('plugin name should begin with p_')

    assetDir += '/' + pluginName

    pluginPackageName = pluginName[2:]

    buildProp = helper.readPropertyFile('build.prop')
    if not buildProp:
        helper.error('build.prop not found')

    print('Build start...')
    rootDir = os.path.abspath(rootDir)
    helper.projectRoot = rootDir
    os.chdir(rootDir)
    
    readGlobalConfig(buildProp)
    
    builder = PluginBuilder()
    builder.start()

    # copy
    if copyPluginToAppAssets:
        dstDir = rootDir + '/app/src/main/assets'
        if not os.path.exists(dstDir):
            helper.watchExecuteCommand('mkdir -p', dstDir)
        cmd = 'cp ' + pluginOutputDir + '/' + pluginName + '.apk ' + dstDir
    
        helper.watchExecuteCommand(cmd, cmdAlias='copy plugin to: ' + dstDir)
