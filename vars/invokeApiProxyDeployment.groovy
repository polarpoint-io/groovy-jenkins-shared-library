/*
 * Surj Bains  <surj@polarpoint.io>
 * InvokePipeline
 * Entry point for all Jobs
 * Called from the Jenkinsfile
 */

import io.polarpoint.workflow.ConfigurationContext
import groovy.json.JsonBuilder



echo("[API PROXY] invoked")


def call(String application, String configuration) {
    ConfigurationContext configurationContext
    node('master') {
        checkout scm

        if (env.BRANCH_NAME!= /(PC|HD|SLR)-\d*/) {
            if (lastCommitIsBumpCommit()) {
                currentBuild.result = 'ABORTED'
                error('Last commit updated the version, aborting the build to prevent a loop.')
            } else {
                echo('Last commit is not a version commit, job continues as normal.')
            }
        }


        configurationContext = new ConfigurationContext(application, readFile(configuration))
        stash name: 'pipelines', includes: 'pipelines/**'
        echo("[API PROXY] calling  with application:" + application)
        echo("[API PROXY] calling  with Branch:  $env.BRANCH_NAME ")
        echo("[API PROXY] calling  with configuration:" + ( new JsonBuilder( configurationContext ).toPrettyString()))

            if (env.BRANCH_NAME =~ /^master/) {
                echo("[API PROXY] master branch being build and tagged:" + application)
                apiProxyDeploymentWorkflow(configurationContext, 'master')
            } else if (env.BRANCH_NAME =~ /^development/) {
                echo("[API PROXY] development branch being build and tagged:" + application)
                apiProxyDeploymentWorkflow(configurationContext, 'development')
            }else if (env.BRANCH_NAME =~ /(PC|HD|SLR)-\d*/) {
                echo("[API PROXY] feature  branch being build and tagged:" + application)
                apiProxyDeploymentWorkflow(configurationContext, env.BRANCH_NAME)
            }
            else {
                echo("[API PROXY] not sure how to continue.")
            }
            echo("[API PROXY] End.")
    }
}


private boolean lastCommitIsBumpCommit() {

    lastCommit = sh([script: 'git log -1', returnStdout: true])
    if (lastCommit.contains("[jenkins-versioned]"))  {
        if (currentBuild.getPreviousBuild().result == 'ABORTED'){

            return false //we know the previous result was aborted to prevent loop
        }
        return true
    } else {
        return false
    }
}


return this
