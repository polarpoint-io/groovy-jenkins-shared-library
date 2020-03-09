/*
 * Surj Bains  <surj@polarpoint.io>
 * InvokePipeline
 * Entry point for all Jobs
 * Called from the Jenkinsfile
 */


import groovy.json.JsonBuilder
import io.polarpoint.workflow.contexts.ServerlessContext

echo("[Pipeline] invoked")
def scmVars

def call(String application, String configuration) {

    timestamps {
        // run this initially on the Jenkins master

        def utils = new io.polarpoint.utils.Utils()
        ServerlessContext serverlessContext
        node('master') {
            properties([disableConcurrentBuilds()])
            scmVars = checkout scm
            GitHubNotify(scmVars, 'Jenkins Build Pipeline', 'jenkinsci/jenkins-pipeline', 'PENDING')
            GitHubNotify(scmVars, 'Sonar Quality Gate', 'jenkinsci/sonar-quality', 'PENDING')


            serverlessContext = new ServerlessContext(application, readFile(configuration), env.WORKSPACE)
            stash name: 'pipelines', includes: 'pipelines/**'
            echo("[Pipeline] calling  with application:" + application)
            echo("[Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
            utils.printColour("[Pipeline] calling with configuration:" + (new JsonBuilder(serverlessContext).toPrettyString()), 'green')
        }

        if (env.BRANCH_NAME =~ /^master/) {
            echo("[Pipeline] master branch being built:" + application)
            serverlessWorkflow(serverlessContext, 'master', scmVars,true)
        } else if (env.BRANCH_NAME =~ /^(release$|development$|hotfix\/|bugfix\/|feature\/)/) {
            echo("[Pipeline] development/release/hotfix/bugfix/feature branch being build and tagged:" + application)
            serverlessWorkflow(serverlessContext, env.BRANCH_NAME, scmVars, true)
        } else if (env.BRANCH_NAME =~ /(BH|PC|HD|SLR)-\d*/) {
            echo("[Pipeline] feature  branch being built:" + application)
            serverlessWorkflow(serverlessContext, env.BRANCH_NAME, scmVars, false)

            node('master') {
                archiveArtifacts artifacts: "pipelines/conf/configuration.json", onlyIfSuccessful: false // archive regardless of success
            }
        } else {
            echo("[Pipeline] not sure how to continue.")
        }
        echo("[Pipeline] End.")


        if (currentBuild.result == 'FAILURE') {
            GitHubNotify(scmVars, 'Jenkins Build Pipeline Failed!', 'jenkinsci/jenkins-pipeline', 'FAILURE')
        } else {
            GitHubNotify(scmVars, 'Jenkins Build Pipeline Succeeded!', 'jenkinsci/jenkins-pipeline', 'SUCCESS')
        }
    }

}

return this
