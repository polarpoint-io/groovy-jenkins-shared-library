/*
 * Surj Bains  <surj@polarpoint.io>
 * InvokeHelmPipeline
 * Entry point for all Jobs
 * Called from the Jenkinsfile
 */


import groovy.json.JsonBuilder
import io.polarpoint.workflow.contexts.HelmContext

echo("[Helm Pipeline] invoked")
def scmVars

def call(String application, String configuration) {

    timestamps {
        // run this initially on the Jenkins master

        def utils = new io.polarpoint.utils.Utils()
        HelmContext helmContext
        node('master') {
            properties([disableConcurrentBuilds()])
            scmVars = checkout scm

            helmContext = new HelmContext(application, readFile(configuration), env.WORKSPACE)
            stash name: 'pipelines', includes: 'pipelines/**'
            echo("[Helm Pipeline] calling  with application:" + application)
            echo("[Helm Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
            utils.printColour("[Pipeline] calling with configuration:" + (new JsonBuilder(helmContext).toPrettyString()), 'green')
        }

        if (env.BRANCH_NAME =~ /^master/) {
            echo("[Helm Pipeline] master branch being build and tagged:" + application)
            helmWorkflow(helmContext, 'master', scmVars,true)
        } else if (env.BRANCH_NAME =~ /^(development$|hotfix\/)/) {
            echo("[Helm Pipeline] development or hotfix branch being build and tagged:" + application)
            helmWorkflow(helmContext, env.BRANCH_NAME, scmVars, true)
        } else if (env.BRANCH_NAME =~ /(BH|PC|HD|SLR)-\d*/) {
            echo("[Helm Pipeline] feature  branch being built:" + application)
            helmWorkflow(helmContext, env.BRANCH_NAME, scmVars, false)
        } else {
            echo("[Helm Pipeline] not sure how to continue.")
        }
        echo("[Helm Pipeline] End.")


        if (currentBuild.result == 'FAILURE') {
            GitHubNotify(scmVars, 'Jenkins Helm Pipeline Failed!', 'jenkinsci/jenkins-pipeline', 'FAILURE')
        } else {

            node('master') {
                archiveArtifacts artifacts: "pipelines/conf/configuration.json", onlyIfSuccessful: false // archive regardless of success
            }

            GitHubNotify(scmVars, 'Jenkins Helm Pipeline Succeeded!', 'jenkinsci/jenkins-pipeline', 'SUCCESS')
        }
    }

}

return this
