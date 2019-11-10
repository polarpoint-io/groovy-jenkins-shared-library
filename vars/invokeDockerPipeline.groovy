/*
 * Surj Bains  <surj@polarpoint.io>
 * InvokePipeline
 * Entry point for all Jobs
 * Called from the Jenkinsfile
 */


import groovy.json.JsonBuilder
import io.polarpoint.workflow.DockerContext

echo("[Docker Pipeline] invoked")
def scmVars

def call(String application, String configuration) {

    def utils = new io.polarpoint.utils.Utils()
    DockerContext configurationContext
    node('master') {

        scmVars = checkout scm
        GitHubNotify(scmVars, 'Jenkins Build Pipeline', 'jenkinsci/jenkins-pipeline', 'PENDING')

        if (env.BRANCH_NAME != /(PC|HD|SLR)-\d*/) {
            if (lastCommitIsBumpCommit()) {
                currentBuild.result = 'ABORTED'
                error('Last commit updated the version, aborting the build to prevent a loop.')
            } else {
                echo('Last commit is not a version commit, job continues as normal.')
            }
        }
        configurationContext = new DockerContext(application, readFile(configuration), env.WORKSPACE)
        stash name: 'pipelines', includes: 'pipelines/**'
        echo("[Docker Pipeline] calling  with application:" + application)
        echo("[Docker Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
        utils.printColour("[Pipeline] calling with configuration:" + (new JsonBuilder(configurationContext).toPrettyString()), 'green')


    if (env.BRANCH_NAME =~ /^master/) {
        echo("[Docker Pipeline] master branch being build and tagged:" + application)
        dockerWorkflow(configurationContext, 'master')
    } else if (env.BRANCH_NAME =~ /^(development|hotfix)/) {
        echo("[Docker Pipeline] development branch being build and tagged:" + application)
        dockerWorkflow(configurationContext, env.BRANCH_NAME)
    } else if (env.BRANCH_NAME =~ /(PC|HD|SLR)-\d*/) {
        echo("[Docker Pipeline] feature  branch being build and tagged:" + application)
        dockerWorkflow(configurationContext, env.BRANCH_NAME)
    } else {
        echo("[Docker Pipeline] not sure how to continue.")
    }
    echo("[Pipeline] End.")
}

    if (currentBuild.result == 'FAILURE') {
        GitHubNotify(scmVars, 'Jenkins Build Pipeline Failed!', 'jenkinsci/jenkins-pipeline', 'FAILURE')
    } else {
        GitHubNotify(scmVars, 'Jenkins Build Pipeline Succeeded!', 'jenkinsci/jenkins-pipeline', 'SUCCESS')
    }


}


private boolean lastCommitIsBumpCommit() {

    lastCommit = sh([script: 'git log -1', returnStdout: true])
    if (lastCommit.contains("[jenkins-versioned]")) {
        if (currentBuild.getPreviousBuild().result == 'ABORTED') {

            return false //we know the previous result was aborted to prevent loop
        }
        return true
    } else {
        return false
    }
}


return this
