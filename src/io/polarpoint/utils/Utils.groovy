package io.polarpoint.utils

import com.vdurmont.semver4j.Semver
import com.vdurmont.semver4j.SemverException
import io.polarpoint.workflow.ConfigurationContext
import io.polarpoint.utils.DirFinder
import groovy.io.FileType
import java.nio.file.Files
import java.nio.file.Paths


@Grab(group = 'com.vdurmont', module = 'semver4j', version = '3.1.0')
@Grab(group = 'org.pegdown', module = 'pegdown', version = '1.4.1')


@NonCPS
def pc_lib_folder () {
    echo '**************CALLING PC_LIB*****************'
    def folders = []
    def dir = new File("${env.WORKSPACE}@libs")
    dir.eachFileRecurse (FileType.DIRECTORIES) { file ->
       folders << file
    }

    echo '**************CALLING PC_LIB FOLDERS  NEW *****************'+folders
    //return folders
    //def FOLDER_NAMES = ["<Library name>", "<UUID>"]
    // def folderreturn

    // def FOLDER_NAMES = ["libs", "<UUID>"]
    // def folder = FOLDER_NAMES[0]
                
    // if ( dir.contains(FOLDER_NAMES[0]) ) {
    //     folder = FOLDER_NAMES[0]
    // }
    // else if ( dir.contains(FOLDER_NAMES[1]) ) {
    //     folder = FOLDER_NAMES[1]
    // }
    // else {
    //     folder = "No directories found"
    // }
    return folders


}



def getCredentialsById(String credsId, String credsType = 'any') {
    def credClasses = [ // ordered by class name
                        sshKey    : com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey.class,
                        cert      : com.cloudbees.plugins.credentials.common.CertificateCredentials.class,
                        password  : com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials.class,
                        any       : com.cloudbees.plugins.credentials.impl.BaseStandardCredentials.class,
                        dockerCert: org.jenkinsci.plugins.docker.commons.credentials.DockerServerCredentials.class,
                        file      : org.jenkinsci.plugins.plaincredentials.FileCredentials.class,
                        string    : org.jenkinsci.plugins.plaincredentials.StringCredentials.class,
    ]
    return com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
            credClasses[credsType],
            jenkins.model.Jenkins.instance
    ).findAll { cred -> cred.id == credsId }[0]
}

def artifactName(String targetBranch, context) {
    return "${context.config.application}-${context.config.version}-${targetBranch}.jar"
}
// TODO this needs to be cleaned up
def artifactNameZip(String targetBranch, context) {
    return "${context.config.application}-${context.config.version}-${targetBranch}.zip"
}

def fullArtifactPath(String targetBranch, context) {
    return "./${context.config.cloudFoundry.artifactPath}/${context.config.archivesBaseName}-${context.config.version}-${targetBranch}.jar"
}

def artifactTag(String targetBranch) {

    targetBranch = branchSubString(targetBranch)

    switch (targetBranch) {
        case 'release':
            return "RELEASE"
        case 'master':
            return "FINAL"
        case 'development':
            return "SNAPSHOT"
        case 'hotfix':
            return "SNAPSHOT"
        default:
            targetBranch = targetBranch.replace('/', '-')
            targetBranch = targetBranch + "-SNAPSHOT"
            return targetBranch


    }
}


def clientCertCN(String env, String appName) {
    return "svc-${appName}-${env}"

}


def branchSubString(String targetBranch) {

    if (targetBranch.contains("hotfix") | targetBranch.contains("release")) {

        if (targetBranch.lastIndexOf("/") > 0) {
            targetBranch = targetBranch.substring(0, targetBranch.lastIndexOf("/"))
            // remove the ticket number so we only have
        }
    } else {

        targetBranch = targetBranch.substring(targetBranch.lastIndexOf("/") + 1)


    }

    return targetBranch
}

def parseJson(String json) {
    return new groovy.json.JsonSlurperClassic().parseText(json)
}

def imageNameDocker(String targetBranch, context) {
    artifactTag = artifactTag(targetBranch)
    return "${context.config.spinnaker.applicationName}:${context.config.version}-${artifactTag}"
}

def determineRepository(String targetBranch, context) {

    targetBranch = branchSubString(targetBranch)

    switch (targetBranch) {
        case 'hotfix':
            return 'pol-packages'
        case 'master':
            return 'pol-releases'
        case 'release':
            return 'pol-releases'
        case 'development':
            return 'pol-packages'
        default:
            return 'pol-packages'
    }
}

def appNameCloudFoundry(String targetBranch, context) {
    return "${context.config.archivesBaseName}-${targetBranch}"
}

String artifactNameCloudFoundry(String targetBranch) {

    switch (targetBranch) {
        case 'master':
            return "dev"
        case 'development':
            return "dev"

        default:
            targetBranch = targetBranch.replace('/', '-')
            return targetBranch

    }
}

def printColour(String message, String colour) {
    ansiColor('xterm') {
        switch (colour) {
            case 'red':
                println "\033[1;31m[Error] ${message}  \033[0m";
                break
            case 'green':
                println "\033[1;32m[OK] ${message}  \033[0m";
                break
            default:
                println "\033[1;32m[OK] ${message}  \033[0m";

        }

    }
}


def parseYml(String path) {

    println "Recursive file YML Starts in UTILs"
    def files = findFiles(glob: 'src/main/config/*.yaml')
    files.each { file ->

        try {
            def data = readYaml file: file.toString()
        } catch (err) {
            error("Can't parse yaml " + err.message)
        }
    }
}


def parseGitHubOrganisation(String pipeline) {
    def organisation = ""

    if (pipeline.lastIndexOf("--") > 0) {
        organisation = pipeline.substring(0, pipeline.indexOf("--"))
        // just the organisation
    } else {
        organisation = ""// use default in Jenkins config
    }
    return organisation
}

/**
 * Returns the version to assign to the artifact being built if the branch
 * supports tagging.
 *
 * @param targetBranch
 * @param context
 * @return
 */
def getNextVersion(String targetBranch, ConfigurationContext context) {

    echo "Getting next version..."

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

    if (gitBranch.lastIndexOf("/") > 0) {
        gitBranch = gitBranch.substring(0, gitBranch.lastIndexOf("/")) // remove the ticket number so we only have
    }                                                              // hotfix
    println "[TAG] gitBranch:*${gitBranch}*"

    String gitDescribe = sh(returnStdout: true, script: 'git describe --tags $(git rev-list --tags --max-count=1) --always --long')
    println "[TAG] git describe: $gitDescribe"

    def gitCSinceTag = sh(returnStdout: true, script: 'git rev-list  `git rev-list --tags --no-walk --max-count=1`..HEAD --count')
    int gitCommitsSinceTag = gitCSinceTag.toInteger()
    println "[TAG] git gitCommitsSinceTag: " + gitCommitsSinceTag

    // Store in env to be used later in the pipeline
    env.GIT_COMMITS_SINCE_TAG = gitCommitsSinceTag

    // if a tag is found
    if (gitDescribe != null && gitDescribe.startsWith("v")) {
        def gitDescribeBits = gitDescribe.split('-')
        String versionString = gitDescribeBits[0].replaceFirst('^v', '')

        try {

            env.LAST_TAG_IN_BRANCH = versionString

            // Allow force starting taggedVersion via config parameter (to solve version synchro issues)
            if(context.config.forceTaggedVersion) {
                echo "Forcing the pipeline via configuration.json (property 'forceTaggedVersion') to consider as last tagged version: ${context.config.forceTaggedVersion}"

                // Force tag from configuration.json
                taggedVersion = new Semver(context.config.forceTaggedVersion)
            } else {
                // Use fetched tag from git
                taggedVersion = new Semver(versionString)
            }

            echo "[TAG] Existing tag: version: ${taggedVersion.toString()}"
        }
        catch (SemverException s) {
            println 'Problem parsing latest tag in git' + s.message
        }

        println 'gitDescribeBits ' + gitDescribeBits
        println '(gitCommitsSinceTag > 0 ) ' + (gitCommitsSinceTag > 0)
        println 'taggedVersion ' + taggedVersion
        // is there have been commits on top of found tag

        if ((gitCommitsSinceTag > 0)) {

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


            println 'nextTag will be: ' + nextVersion

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

    return nextVersion
}

/**
 * Tries to fetch an artifact from Nexus. If the artifact is found, it is handled in the same way
 * as it would if it had been built.
 *
 * @param targetBranch
 * @param context
 * @param artifactVersion
 * @return
 */
def downloadArtifact(String targetBranch, ConfigurationContext context, final String artifactVersion) {
    def tag = artifactTag(targetBranch) // use -RELEASE or -SNAPSHOT depending on the branch
    def nexusUrl = 'artifact.pohzn.com'
    def credentialsID = 'svc-nexus-user'
    def groupId = context.config.group
    def repository = determineRepository(targetBranch, context)
    def status = null

    try {

        def finalArtifactAlreadyExists = false
        def lastTagInBranch = artifactVersion
        def artifact = "${context.config.application}-${lastTagInBranch}-${tag}.jar"

        echo "Trying to download artifact from Nexus..."

        def groupIdWithSlashes = groupId.replace(".", "/")
        def artifactURL = "https://${nexusUrl}/repository/${repository}/${groupIdWithSlashes}/${context.config.application}/${lastTagInBranch}-${tag}/${artifact}"

        echo "Artifact URL: ${artifactURL}"

        def response = httpRequest(
                url: "${artifactURL}",
                validResponseCodes: "200, 404",
                authentication: credentialsID,
                outputFile: "build/libs/${artifact}")

        status = response.status

        echo "Response Status: ${status}"

        if (status == 200) {

            if (artifactVersion != context.config.version) {
                echo "Previous artifact fetched. Reversioning it..."

                // Patching (non code changes). Fetched artifact will be reused and reversioned.
                def currentArtifactName = "${context.config.application}-${context.config.version}-${tag}.jar"

                // Update artifact version
                def command = "mv ./build/libs/${artifact} ./build/libs/${currentArtifactName}"

                sh(returnStdout: false, script: command)
            }

            stash name: 'build',
                    includes: "build/libs/*,**/pipelines/**,Dockerfile,Jenkinsfile,pom.xml"
            stash name: 'artifacts',
                    includes: "build/libs/*.jar,**/pipelines/**,Dockerfile,Jenkinsfile,pom.xml"
            archiveArtifacts artifacts: "build/libs/*.jar",
                    onlyIfSuccessful: true
        }

    } catch (err) {
        error("Failed trying to download artifact from Nexus" + err.message)
    }

    return status
}

/**
 * Check the changes in repository since last artifact was built and identifies if that changes
 * imply that a new artifact must be built (for instance, changes in source code).
 *
 * @return
 */
def commitsImplyBuilding() {

    echo "Checking if changes in repository imply building artifact..."

    def commitsImplyBuilding = false

    def String command = null

    if (env.BRANCH_NAME =~ /^(release$)/) {

        command = "git diff --name-only \$(git rev-list release --grep jenkins-versioned.*-release-.d* --max-count=1)..HEAD"

    } else if (env.BRANCH_NAME =~ /^(master$)/) {

        // Obtain changed files since last merge to the branch
        command = "git diff --name-only \$(git rev-list --min-parents=2 --first-parent --max-count=1 HEAD)..\$(git rev-list --min-parents=2 --first-parent --max-count=1 --skip=1 HEAD)"

    }

    def changedFiles = sh(returnStdout: true, script: command).tokenize("\n")

    echo "changedFiles = ${changedFiles}"

    // Typical changed files should have 2 or more entries (configuration.json and the rest of changed files).
    // If not, force building to avoid trusting intermediate commits.
    if(changedFiles.size() < 2) {
        commitsImplyBuilding = true

        echo "Changes contain less than two files. Force rebuilding..."
    } else {

        // Iterate over changes until a change that implies building the artifact is found.
        for (int i = 0; !commitsImplyBuilding && (i < changedFiles.size()); ++i) {

            // Files that don't imply building if changed
            commitsImplyBuilding = !("Dockerfile" == changedFiles[i] ||
                    "pipelines/conf/deployment.yaml" == changedFiles[i] ||
                    "pipelines/conf/deployment-docker.yaml" == changedFiles[i] ||
                    "pipelines/conf/configuration.json" == changedFiles[i])
        }

    }


    return commitsImplyBuilding
}

/**
 * Tags the artifact according to the branching strategy.
 * Needs to find in env.NEXT_VERSION the version to apply to the artifact (if any).
 *
 * @param targetBranch
 * @param context
 * @return
 */
def tag(String targetBranch, ConfigurationContext context) {

    final def ENV_NEXT_VERSION = env.NEXT_VERSION
    Semver nextVersion = (ENV_NEXT_VERSION != "null") ? new Semver(ENV_NEXT_VERSION) : null

    def REPO = scm.userRemoteConfigs.getAt(0).getUrl()
    REPO = REPO - 'https://'

    String currentBranch = targetBranch

    if (nextVersion != null) {

        checkout([
                $class           : 'GitSCM',
                branches         : [[name: currentBranch]],
                extensions       : [[$class: 'CloneOption', depth: 0, noTags: false, reference: '', shallow: false], [$class: 'LocalBranch', localBranch: '']],
                userRemoteConfigs: scm.userRemoteConfigs,
                repository       : scm.repositories,
        ])

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
    archiveArtifacts artifacts: "pipelines/conf/configuration.json", onlyIfSuccessful: false
    // archive regardless of success
}
