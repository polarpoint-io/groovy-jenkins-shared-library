/*
 * Surj Bains  <surj@polarpoint.io>
 * InvokeGenericWorkflow
 * Entry point for all Jobs
 * Called from the Jenkinsfile
 */

import io.polarpoint.workflow.*
import groovy.json.JsonBuilder



echo("[Generic Workflow] invoked")


def call(String application, String configuration) {
    GenericContext genericContext
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

        genericContext =new GenericContext(application, readFile(configuration), env.WORKSPACE)
        stash name: 'pipelines', includes: 'pipelines/**'
        echo("[Generic Workflow] calling  with application:" + application)
        echo("[Generic Workflow] calling  with Branch:  $env.BRANCH_NAME ")
        echo("[Generic Workflow] calling  with configuration:" + ( new JsonBuilder( genericContext ).toPrettyString()))

            if (env.BRANCH_NAME =~ /^master/) {
                echo("[Generic Workflow] master branch being build and tagged:" + application)
                genericWorkflow(genericContext, 'master')
            } else if (env.BRANCH_NAME =~ /^development/) {
                echo("[Generic Workflow] development branch being build and tagged:" + application)
                genericWorkflow(genericContext, 'development')
            }else if (env.BRANCH_NAME =~ /(PC|HD|SLR)-\d*/) {
                echo("[Generic Workflow] feature  branch being build and tagged:" + application)
                genericWorkflow(genericContext, env.BRANCH_NAME)
            }
            else {
                echo("[Generic Workflow] not sure how to continue.")
            }
            echo("[Generic Workflow] End.")
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
