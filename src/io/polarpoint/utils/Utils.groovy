package io.polarpoint.utils

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

    targetBranch= branchSubString(targetBranch)

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


def branchSubString(String targetBranch)
{

    if (targetBranch.contains("hotfix") | targetBranch.contains("release")) {

        if (targetBranch.lastIndexOf("/") > 0) {
            targetBranch = targetBranch.substring(0, targetBranch.lastIndexOf("/"))
            // remove the ticket number so we only have
        }
    }
    else
    {

        targetBranch = targetBranch.substring(targetBranch.lastIndexOf("/")+1)


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

    targetBranch= branchSubString(targetBranch)

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


def parseGitHubOrganisation(String pipeline)
{
    def organisation = ""

        if (pipeline.lastIndexOf("--") > 0) {
            organisation = pipeline.substring(0, pipeline.indexOf("--"))
            // just the organisation
        }else
        {
            organisation = ""// use default in Jenkins config
        }
    return organisation
}


