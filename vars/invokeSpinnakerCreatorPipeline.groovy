/*
 * Surj Bains  <surj@polarpoint.io>
 * invokeSpinnakerCreatorPipeline
 * Called from the Jenkinsfile
 */


import groovy.json.JsonBuilder
import io.polarpoint.workflow.contexts.SpinnakerCreatorContext

echo("[SpinnakerCreator Pipeline] invoked")
def scmVars

def call(String application, String configuration) {

    timestamps {
        // run this initially on the Jenkins master

        def utils = new io.polarpoint.utils.Utils()
        SpinnakerCreatorContext spinnakerCreatorContext
        node('master') {
            properties([disableConcurrentBuilds()])
            scmVars = checkout scm
            GitHubNotify(scmVars, 'Jenkins Build Pipeline', 'jenkinsci/jenkins-pipeline', 'PENDING')
            GitHubNotify(scmVars, 'Sonar Quality Gate', 'jenkinsci/sonar-quality', 'PENDING')


            spinnakerCreatorContext = new SpinnakerCreatorContext(application, readFile(configuration), env.WORKSPACE)
            stash name: 'pipelines', includes: 'pipelines/**'
            echo("[SpinnakerCreator Pipeline] calling  with application:" + application)
            echo("[SpinnakerCreator Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
            utils.printColour("[Pipeline] calling with configuration:" + (new JsonBuilder(spinnakerCreatorContext).toPrettyString()), 'green')
        }

        if (env.BRANCH_NAME =~ /^master/) {
            echo("[SpinnakerCreator Pipeline] master branch being build and tagged:" + application)
            spinnakerCreatorWorkflow(spinnakerCreatorContext, 'master', scmVars,true)
        } else if (env.BRANCH_NAME =~ /^(development$|hotfix\/)/) {
            echo("[SpinnakerCreator Pipeline] development or hotfix branch being build and tagged:" + application)
            spinnakerCreatorWorkflow(spinnakerCreatorContext, env.BRANCH_NAME, scmVars, true)
        } else if (env.BRANCH_NAME =~ /(BH|PC|HD|SLR)-\d*/) {
            echo("[SpinnakerCreator Pipeline] feature  branch being built:" + application)
            spinnakerCreatorWorkflow(spinnakerCreatorContext, env.BRANCH_NAME, scmVars, false)
        } else {
            echo("[SpinnakerCreator Pipeline] not sure how to continue.")
        }
        echo("[SpinnakerCreator Pipeline] End.")


        if (currentBuild.result == 'FAILURE') {
            GitHubNotify(scmVars, 'Jenkins SpinnakerCreatorContext Pipeline Failed!', 'jenkinsci/jenkins-pipeline', 'FAILURE')
        } else {
            GitHubNotify(scmVars, 'Jenkins SpinnakerCreatorContext Pipeline Succeeded!', 'jenkinsci/jenkins-pipeline', 'SUCCESS')
        }
    }

}

return this
