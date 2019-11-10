/*
 * Surj Bains  <surj@polarpoint.io>
 * InvokePipeline
 * Entry point for all Jobs
 * Called from the Jenkinsfile
 */


import groovy.json.JsonBuilder
import io.polarpoint.workflow.NodeContext

echo("[Node Pipeline] invoked")
def scmVars

def call(String application, String configuration) {

    def utils = new io.polarpoint.utils.Utils()
    NodeContext configurationContext
    node('master') {
        properties([disableConcurrentBuilds()])
        scmVars = checkout scm
        GitHubNotify(scmVars, 'Jenkins Build Pipeline', 'jenkinsci/jenkins-pipeline', 'PENDING')

        configurationContext = new NodeContext(application, readFile(configuration), env.WORKSPACE)

        stash name: 'pipelines', includes: 'pipelines/**'
        echo("[Node Pipeline] calling  with application:" + application)
        echo("[Node Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
        utils.printColour("[Pipeline] calling with configuration:" + (new JsonBuilder(configurationContext).toPrettyString()), 'green')
    }

    if (env.BRANCH_NAME =~ /^master/) {
        echo("[Node Pipeline] master branch being build and tagged:" + application)
        nodeWorkflow(configurationContext, 'master')
    } else if (env.BRANCH_NAME =~ /^(release$|development$|hotfix\/|bugfix\/|feature\/)/) {        
        echo("[Node Pipeline] development branch being build and tagged:" + application)
        nodeWorkflow(configurationContext, env.BRANCH_NAME)
    } else if (env.BRANCH_NAME =~ /(PC|HD|BH|SLR)-\d*/) {
        echo("[Node Pipeline] feature  branch being build and tagged:" + application)
        nodeWorkflow(configurationContext, env.BRANCH_NAME)

        node('master') {
            archiveArtifacts artifacts: "pipelines/conf/configuration.json", onlyIfSuccessful: false // archive regardless of success
        }
    } else {
        echo("[Node Pipeline] not sure how to continue.")
    }
    echo("[Pipeline] End.")

    if (currentBuild.result == 'FAILURE') {
        GitHubNotify(scmVars, 'Jenkins Build Pipeline Failed!', 'jenkinsci/jenkins-pipeline', 'FAILURE')
    } else {
        GitHubNotify(scmVars, 'Jenkins Build Pipeline Succeeded!', 'jenkinsci/jenkins-pipeline', 'SUCCESS')
    }
}

private boolean lastCommitIsBumpCommit() {

    lastCommit = sh([script: 'git log -1  | sed -n \'5p\'', returnStdout: true])
    if (lastCommit.contains("[jenkins-versioned]")) {


        return true
    } else {
        return false
    }
}


return this
