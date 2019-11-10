/*
 * Surj Bains  <surj@polarpoint.io>
 * invokeSpinnakerSyncPipeline
 * Called from the Jenkinsfile
 */


import groovy.json.JsonBuilder
import io.polarpoint.workflow.contexts.SpinnakerSyncContext

echo("[SpinnakerSync Pipeline] invoked")
def scmVars

def call(String application, String configuration) {

    timestamps {
        // run this initially on the Jenkins master

        def utils = new io.polarpoint.utils.Utils()
        SpinnakerSyncContext spinnakerSyncContext
        node('master') {
            properties([disableConcurrentBuilds()])
            scmVars = checkout scm
            GitHubNotify(scmVars, 'Jenkins Build Pipeline', 'jenkinsci/jenkins-pipeline', 'PENDING')
            GitHubNotify(scmVars, 'Sonar Quality Gate', 'jenkinsci/sonar-quality', 'PENDING')


            spinnakerSyncContext = new SpinnakerSyncContext(application, readFile(configuration), env.WORKSPACE)
            stash name: 'pipelines', includes: 'pipelines/**'
            echo("[SpinnakerSync Pipeline] calling  with application:" + application)
            echo("[SpinnakerSync Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
            utils.printColour("[Pipeline] calling with configuration:" + (new JsonBuilder(spinnakerSyncContext).toPrettyString()), 'green')
        }

        if (env.BRANCH_NAME =~ /^master/) {
            echo("[SpinnakerSync Pipeline] master branch being build and tagged:" + application)
            spinnakerSyncWorkflow(spinnakerSyncContext, 'master', scmVars,true)
        } else if (env.BRANCH_NAME =~ /^(development$|hotfix\/)/) {
            echo("[SpinnakerSync Pipeline] development or hotfix branch being build and tagged:" + application)
            spinnakerSyncWorkflow(spinnakerSyncContext, env.BRANCH_NAME, scmVars, true)
        } else if (env.BRANCH_NAME =~ /(BH|PC|HD|SLR)-\d*/) {
            echo("[SpinnakerSync Pipeline] feature  branch being built:" + application)
            spinnakerSyncWorkflow(spinnakerSyncContext, env.BRANCH_NAME, scmVars, false)
        } else {
            echo("[SpinnakerSync Pipeline] not sure how to continue.")
        }
        echo("[SpinnakerSync Pipeline] End.")


        if (currentBuild.result == 'FAILURE') {
            GitHubNotify(scmVars, 'Jenkins SpinnakerSync Pipeline Failed!', 'jenkinsci/jenkins-pipeline', 'FAILURE')
        } else {
            GitHubNotify(scmVars, 'Jenkins SpinnakerSync Pipeline Succeeded!', 'jenkinsci/jenkins-pipeline', 'SUCCESS')
        }
    }

}

return this
