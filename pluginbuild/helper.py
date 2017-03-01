'''
Created on Mar 7, 2016

@author: lqp
'''

import os
import re
import sys
import subprocess as subproc

projectRoot = ''

logFile = None


def buildLog(msg):
    if logFile:
        logFile.write(msg + '\n')

    print(msg)


# tool module code start
def watchExecuteCommand(cmd, args='', cmdAlias=None):
    if not cmdAlias:
        cmdAlias = cmd
    else:
        cmdAlias = '[' + cmdAlias + ']'

    buildLog('Cmd: ' + cmdAlias + " " + args)

    code = subproc.Popen(cmd + " " + args, shell=True).wait()
    if code:
        print 'Fail: ' + str(code)
        exit()
    else:
        print 'Success !!!'


def collectFilesByPattern(folder, include=None, exclude=None):
    if not os.path.isdir(folder):
        buildLog('collectFilesByPattern: folder not exist: ' + folder)
        return []

    srcFiles = []

    fileList = os.listdir(folder)
    for i in fileList:
        i = folder + '/' + i
        if os.path.isdir(i):
            srcFiles += collectFilesByPattern(i, include, exclude)
        else:
            if include and not re.match(include, i):
                continue

            if exclude and re.match(exclude, i):
                continue

            srcFiles.append(i)

    return srcFiles


def strUntil(string, endChar):
    res = ''

    for c in string:
        if c == endChar:
            break;

        res += c

    return res


def error(string):
    sys.exit("Error: " + string)
    return


def stripQuotedString(string):
    res = string

    if (string.startswith('\'') and string.endswith('\'')) \
            or (string.startswith('"') and string.endswith('"')):
        res = string[1:-1]

    return res


def stripProjectString(string):
    res = string

    if (string.startswith('\'') and string.endswith('\'')) \
            or (string.startswith('"') and string.endswith('"')):
        res = string[1:-1]

    if res.startswith(":"):
        res = res[1:]

    return res


def expandJarPath(libString, pathPrefix=None):
    pathList = []

    splitItems = libString.split(',')
    for item in splitItems:
        if 'dir' in item:
            dirItem = item.split(':')[1].strip()
            dirItem = stripQuotedString(dirItem)
        elif 'include' in item:
            pass

    libDir = dirItem
    if pathPrefix:
        libDir = projectRoot + '/' + pathPrefix + '/' + libDir
    else:
        libDir = os.getcwd() + '/' + libDir

    if not os.path.exists(libDir):
        # print("libPath: " + libDir + " not exist")
        return

    libFiles = os.listdir(libDir)
    for libFile in libFiles:
        if libFile.endswith('.jar'):
            pathList.append(libDir + '/' + libFile)

    return pathList


def deDuplicateStringList(srcList):
    if len(srcList) == 0:
        return srcList

    item = srcList.pop()
    dstList = []

    while item:

        if item not in dstList:
            dstList.append(item);

        if len(srcList) > 0:
            item = srcList.pop()
        else:
            item = None

    srcList += dstList
    return


def readPropertyFile(path):
    dicts = {}

    lines = []
    with open(path) as fd:
        for line in fd.readlines():
            if re.match(r'^\s*#', line) or '=' not in line:
                continue
            else:
                # print('append property line:' + line)
                if line.endswith('\n'):
                    line = line[:len(line) - 1]
                    
                lines.append(line)

    for line in lines:
        items = line.split('=')

        dicts[items[0].strip()] = items[1].strip()

    return dicts

    # tool module code end
