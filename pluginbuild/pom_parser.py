'''
Created on Feb 26, 2016

@author: lqp
'''

import os
import xml.sax


class RepoItem():
    def __init__(self):
        self.groupId = ''
        self.artifactId = ''
        self.version = ''
        self.fileType = ''
        self.scope = ''
        self.depItems = []

    def __repr__(self):
        return 'groupId: ' + self.groupId + ', ' \
               + 'artifactId: ' + self.artifactId + ', ' \
               + 'version: ' + self.version + ', ' \
               + 'fileType: ' + self.fileType + ', ' \
               + 'deps: ' + str(self.depItems) + ', ' \
               + 'scope: ' + self.scope

    def toFolder(self):
        return self.groupId.replace('.', '/') \
               + '/' + self.artifactId \
               + '/' + self.version

    def toPomFileName(self, rootDir=None):
        retVal = self.toFolder() + '/' \
                 + self.artifactId \
                 + '-' + self.version \
                 + '.pom'

        if rootDir:
            retVal = rootDir + '/' + retVal

        return retVal

    def toLibFile(self, rootDir=None):
        postFix = 'jar'
        if self.fileType:
            postFix = self.fileType

        retVal = self.toFolder() + '/' \
                 + self.artifactId \
                 + '-' + self.version \
                 + '.' + postFix

        if rootDir:
            retVal = rootDir + '/' + retVal

        return retVal

    def __eq__(self, other):
        return self.groupId == other.groupId \
               and self.artifactId == other.artifactId \
               and self.version == other.version

    def __ne__(self, other):
        return not self.__eq__(other)


class PomHandler(xml.sax.ContentHandler):
    def __init__(self):
        self.repoItem = RepoItem()
        self.depItems = self.repoItem.depItems
        self.inDepBlock = False

    # Call when an element starts
    def startElement(self, tag, attributes):
        self.lastTag = tag

        if tag == 'dependencies':
            self.inDepBlock = True

        if not self.inDepBlock:
            return

        if self.lastTag == 'dependency':
            self.curItem = RepoItem()
            self.depItems.append(self.curItem)
        return

    # Call when an elements ends
    def endElement(self, tag):
        self.lastTag = None

        if tag == 'dependencies':
            self.inDepBlock = False

        return

    # Call when a character is read
    def characters(self, content):
        if not self.lastTag:
            return

        if not self.inDepBlock:
            if self.lastTag == 'groupId':
                self.repoItem.groupId = content
            elif self.lastTag == 'artifactId':
                self.repoItem.artifactId = content
            elif self.lastTag == 'version':
                self.repoItem.version = content
            elif self.lastTag == 'packaging':
                self.repoItem.fileType = content

            return

        if self.lastTag == 'groupId':
            self.curItem.groupId = content
        elif self.lastTag == 'artifactId':
            self.curItem.artifactId = content
        elif self.lastTag == 'version':
            self.curItem.version = content
        elif self.lastTag == 'scope':
            self.curItem.scope = content

        return


def collectRepoAndDeps(repoItem, rootDir=None):
    repoPomFile = repoItem.toPomFileName(rootDir)

    rootPom = parseRepoDeps(repoPomFile)

    poms = []
    poms.append(rootPom)

    workQueue = []

    curPom = rootPom

    while curPom:
        for depPomItem in curPom.depItems:
            depPom = parseRepoDeps(depPomItem.toPomFileName(rootDir))
            if depPom in poms:
                continue

            poms.append(depPom)
            workQueue.append(depPom)

        if len(workQueue) == 0:
            break

        curPom = workQueue.pop()

    for item in poms:
        item.depItems = None

    return poms


def parseRepoDeps(pomFile):
    print 'parseRepoDep: ' + str(pomFile)
    if not os.path.exists(pomFile):
        print('pomFile not exist: ' + pomFile)
        return

    # create an XMLReader
    parser = xml.sax.make_parser()
    # turn off namepsaces
    parser.setFeature(xml.sax.handler.feature_namespaces, 0)

    # override the default ContextHandler
    handler = PomHandler()
    parser.setContentHandler(handler)
    parser.parse(pomFile)

    print(handler.repoItem)
    return handler.repoItem
