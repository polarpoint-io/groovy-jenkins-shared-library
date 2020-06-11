/*
 * Surj Bains  <surj@polarpoint.io>
 * This class applies semantic versioning to our source repos
 *
 *
 */

import com.cloudbees.groovy.cps.NonCPS
import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.SemverException
import groovy.text.SimpleTemplateEngine
import io.polarpoint.workflow.ConfigurationContext
import io.polarpoint.workflow.DockerContext
import io.polarpoint.workflow.GenericContext
import io.polarpoint.workflow.MongoContext
import io.polarpoint.workflow.NodeContext
import io.polarpoint.workflow.ZookeeperContext
import io.polarpoint.workflow.contexts.HelmContext
import io.polarpoint.workflow.contexts.DotNetContext
import io.polarpoint.workflow.contexts.ServerlessContext
import io.polarpoint.workflow.contexts.Javav_0_2_0ConfigurationContext

@Grab(group = 'com.vdurmont', module = 'semver4j', version = '2.2.1')
@Grab(group = 'org.pegdown', module = 'pegdown', version = '1.4.1')
import org.pegdown.PegDownProcessor

// all this java'ish groovy!

def call(String targetBranch, Javav_0_2_0ConfigurationContext context) {
    def configContext = new ConfigurationContext(context.application, context.config)
    this.call(targetBranch, configContext)
}


def call(String targetBranch, ServerlessContext context) {
    def configContext = new ConfigurationContext(context.application, context.config)
    this.call(targetBranch, configContext)
}

def call(String targetBranch, DotNetContext context) {
    def configContext = new ConfigurationContext(context.application, context.config)
    this.call(targetBranch, configContext)
}

// all this java'ish groovy, you love it!
def call(String targetBranch, ZookeeperContext context) {
    def configContext = new ConfigurationContext(context.application, context.config)
    this.call(targetBranch, configContext)
}

def call(String targetBranch, MongoContext context) {
    def configContext = new ConfigurationContext(context.application, context.config)
    this.call(targetBranch, configContext)
}

def call(String targetBranch, DockerContext context) {
    def configContext = new ConfigurationContext(context.application, context.config)
    this.call(targetBranch, configContext)
}


def call(String targetBranch, GenericContext context) {
    def configContext = new ConfigurationContext(context.application, context.config)
    this.call(targetBranch, configContext)
}

def call(String targetBranch, NodeContext context) {
    def configContext = new ConfigurationContext(context.application, context.config)
    this.call(targetBranch, configContext)
}


def call(String targetBranch, HelmContext context) {
    def configContext = new ConfigurationContext(context.application, context.config)
    this.call(targetBranch, configContext)
}

def call(String targetBranch, ConfigurationContext context) {

    // have there been commits on branch since latest tag
    Boolean hasCommits = false
    // the latest tag in git
    Semver taggedVersion
    // the next version to use
    Semver nextVersion = null

    Semver configuredVersion = new Semver(context.config.version)
    // jenkins build number
    buildNumber = env.BUILD_NUMBER ?: "0"

    def applicationName = context.config.application

    echo "[TAG] Current configured version in configuration.json: " + configuredVersion.toString()

    def REPO = scm.userRemoteConfigs.getAt(0).getUrl()
    REPO = REPO - 'https://'

    String currentBranch = targetBranch

    //ensure we take a deep clone to get the tags (if any)
    checkout([
            $class           : 'GitSCM',
            branches         : [[name: currentBranch]],
            extensions       : [[$class: 'CloneOption', depth: 0, noTags: false, reference: '', shallow: false], [$class: 'LocalBranch', localBranch: '']],
            userRemoteConfigs: scm.userRemoteConfigs,
            repository       : scm.repositories,

    ])

    sh 'git config --global push.default simple'
    def gitBranch = sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()

    if (gitBranch.lastIndexOf("/") > 0 ) {
        gitBranch = gitBranch.substring(0, gitBranch.lastIndexOf("/")) // remove the ticket number so we only have
    }                                                              // hotfix
    println "[TAG] gitBranch:*${gitBranch}*"

    String gitDescribe = sh(returnStdout: true, script: 'git describe --tags $(git rev-list --tags --max-count=1) --always --long')
    println "[TAG] git describe: $gitDescribe"

    def gitCSinceTag =sh(returnStdout: true, script: 'git rev-list  `git rev-list --tags --no-walk --max-count=1`..HEAD --count')
    int gitCommitsSinceTag = gitCSinceTag.toInteger()
    println "[TAG] git gitCommitsSinceTag: "+ gitCommitsSinceTag


    // if a tag is found
    if (gitDescribe != null && gitDescribe.startsWith("v")) {
        def gitDescribeBits = gitDescribe.split('-')
        String versionString = gitDescribeBits[0].replaceFirst('^v', '')
        try {
            taggedVersion = new Semver(versionString)
            echo "[TAG] Existing tag: version: ${taggedVersion.toString()}"
        }
        catch (SemverException s) {
            println 'Problem parsing latest tag in git' + s.message
        }

        println 'gitDescribeBits ' + gitDescribeBits
        println '(gitCommitsSinceTag > 0 ) ' + (gitCommitsSinceTag > 0 )
        println 'taggedVersion '+ taggedVersion
        // is there have been commits on top of found tag

        if ((gitCommitsSinceTag > 0 )) {

            switch (gitBranch.toLowerCase()) {
                case "hotfix":
                    nextVersion = taggedVersion.nextPatch()
                case "bugfix":
                    nextVersion = taggedVersion.nextPatch()
                    break;
                case "development":
                    nextVersion = taggedVersion.nextMinor()
                    break;
                case "release":
                    nextVersion = taggedVersion.nextMinor()
                    break;
                case "master":
                    break;
                default:
                    println 'Branch not suitable for tagging'

            }
            println 'nextTag will be: '+nextVersion

            // no changes since last tag.
        } else {
            println 'No changes found in git since last tag: ' + taggedVersion.toString()
            nextVersion = null
        }

    } else {
        // edge case where no tag exists in SCM
        println '[TAG] Can find any tags in git so starting with a default one!'

        if (configuredVersion != null) {
            nextVersion = configuredVersion.nextMinor()
            taggedVersion = new Semver('0.1.0')
        } else {
            println '[TAG] setting version tags '
            nextVersion = new Semver('0.1.0')
            taggedVersion = new Semver('0.1.0')

        }
    }

    if (nextVersion != null) {

//        switch (gitBranch.toLowerCase()) {
//            case "hotfix": // allow mismatched tagged and configuration version
//                break;
//            default:
//
//                if (!taggedVersion.isEqualTo(configuredVersion)) {
//                    error("Latest tag + " + taggedVersion + " in git does not match configuration.json " + configuredVersion)
//                }
//        }


        nextVersionTag = 'v' + nextVersion.toString()
        echo "[TAG] next version TAG to use: ${nextVersionTag}"

        // persist our version number to the json configuration file
        def jsonBuilder = new groovy.json.JsonBuilder(context)
        jsonBuilder.content.config.version = nextVersion.toString()
        json = context.asJsonString()
        jsonBuilder = null
        writeFile file: "${env.WORKSPACE}/pipelines/conf/configuration.json", text: json

        sh "cat ${env.WORKSPACE}/pipelines/conf/configuration.json"

        if (fileExists("${env.WORKSPACE}/package.json")) {
            this.updatePackageJson(nextVersion)
            sh "git add package.json"
            stash name: 'package.json', includes: 'package.json'

        }

        if (fileExists("Chart.yaml")) {
            echo "There is Chart.yaml. Versioning..."

            def chartName = 'Chart.yaml'
            def data = readYaml file: chartName

            echo "Chart.yaml data: ${data}"

            data.version = nextVersion.toString()

            sh "rm $chartName"
            writeYaml file: chartName, data: data
            // Rewrite configuration.json inside chart project directory
            writeFile file: "pipelines/conf/configuration.json", text: json

            echo "Packaging..."
            sh "git rm *.tgz"
            sh "helm package ."

            sh "git add pipelines/conf/configuration.json"
            sh "git add Chart.yaml"
            sh "git add *.tgz"
        }

        // git tag the version
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: '43cee12e-c6be-41e5-b5c3-79e3c47c1292', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASS']]) {
            //pipelines/templates
            def templatePath = "${env.WORKSPACE}/pipelines/templates"
            // create release notes for changes
            //releaseNotes(applicationName,templatePath)
            //config = sh(returnStdout: true, script: "git add ${env.WORKSPACE}/CHANGELOG.md")
            sh 'git config --global user.email \"jenkins@mycnets.com\"'
            sh 'git config --global user.name \"Jenkins Server\"'
            config = sh(returnStdout: true, script: "git add pipelines/conf/configuration.json")
            configCommit = sh(returnStdout: true, script: "git commit -m '[jenkins-versioned] ${nextVersionTag} + ${env.BUILD_TAG}' ")
            tagVersion = sh(returnStdout: true, script: "git tag -a ${nextVersionTag} -m '${nextVersionTag}' ")
            tagPush = sh(returnStdout: true, script: "git push https://${GITHUB_USER}:${GITHUB_PASS}@${REPO}  && git push https://${GITHUB_USER}:${GITHUB_PASS}@${REPO} --tags  ")


        }

    } else {

        println "[TAG] not tagging this build because there are no commits since the previous tag"

        // hack for node builds
        if (fileExists("${env.WORKSPACE}/package.json")) {
            stash name: 'package.json', includes: 'package.json'
        }

        env.no_new_commits = true

        echo "env.no_new_commits = ${env.no_new_commits}"

    }
    archiveArtifacts artifacts: "pipelines/conf/configuration.json", onlyIfSuccessful: false // archive regardless of success
}


def updatePackageJson(nextVersion, packageFile = 'package.json') {
    json = readFile(packageFile)
    def packageJson = new groovy.json.JsonSlurperClassic().parseText(json)
    packageJson.version = nextVersion.toString()
    jsonOut = new groovy.json.JsonBuilder(packageJson).toPrettyString()
    writeFile file: packageFile, text: jsonOut
}

def readTags() {
    def tags = []
    tags = sh(returnStdout: true, script: 'git tag -l --sort=-version:refname').split("\n")
    tags
}

def readTagMessage(String tag) {
    def message = []
    message = sh(returnStdout: true, script: "git cat-file tag $tag").split("\n")
    message
}


def readCommitsbetweenTagMessage(String startTag, endTag) {
    def message = []
    message = sh(returnStdout: true, script: "git log --pretty=format:\"%h %ad%x09%an%x09%s\" --date=short $startTag...$endTag").split("\n")
    message
}


def releaseNotes(String application, String templatePath) {

    def releaseNotes = new File(templatePath + '/releaseNotes.md')

    writeFile file: releaseNotes.toString(), text: ""
    def versions = ""

    def tags = readTags()
    def tagSize = tags.size()
    def tagRange
    0.step(tagSize, 1)
            {
                if ((tagSize - 1) == it) {
                    tagRange = tags[it..it]
                    releaseNotes << "# ${tagRange[0]}<a name='$tagRange[0]'></a>\n"

                    def message = readCommitsbetweenTagMessage(tagRange[0], tagRange[0])
                    message.each { releaseNotes << "- $it\n<br>" }
                    releaseNotes << "\n"

                } else {
                    tagRange = tags[it..(it + 1)]
                    releaseNotes << "# ${tagRange[0]}<a name='$tagRange[0]'></a>\n"
                    def message = readCommitsbetweenTagMessage(tagRange[1], tagRange[0])
                    message.each { releaseNotes << "- $it\n" }
                    releaseNotes << "\n"
                }

            }

//        tags.each {tag ->
//            versions += "- [$tag](#$tag)\n <br>"
//        }
//
//        tags.each {tag ->
//            releaseNotes << "# ${tag}<a name='$tag'></a>\n"
//            def message = readTagMessage(tag)
//            message.each{releaseNotes << "$it\n"}
//            releaseNotes << "\n"
//        }


    def pdp = new PegDownProcessor()
    def engine = new SimpleTemplateEngine()
    println 'Using template file at :' + templatePath
    def template = engine.createTemplate(new File(templatePath + "releaseNotes.tpl"))
    def markdown = [releaseNotes: pdp.markdownToHtml(new File(templatePath + "releaseNotes.md").text), application: application, versions: pdp.markdownToHtml(versions)]
    def result = template.make(markdown)
    def releaseNotesHTML = new File(templatePath + 'releaseNotes.html')
    releaseNotesHTML.write(result.toString())

}


return this
