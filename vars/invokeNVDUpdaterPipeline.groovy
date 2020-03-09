/*
 * Surj Bains  <surj@polarpoint.io>
 * InvokeGitRobPipeline
 * Entry point for NVDUpdaterPipeline Job
 * Called from the Jenkinsfile
 */


import groovy.json.JsonBuilder
import io.polarpoint.workflow.contexts.NVDUpdateContext

echo("[Pipeline] invoked")
def scmVars

def call(String application, String configuration) {

    timestamps {
        // run this initially on the Jenkins master

        def utils = new io.polarpoint.utils.Utils()
        NVDUpdateContext configurationContext
        node('master') {
            properties([disableConcurrentBuilds()])
            scmVars = checkout scm
            GitHubNotify(scmVars, 'Jenkins Build Pipeline', 'jenkinsci/jenkins-pipeline', 'PENDING')
            GitHubNotify(scmVars, 'Sonar Quality Gate', 'jenkinsci/sonar-quality', 'PENDING')


            configurationContext = new NVDUpdateContext(application, readFile(configuration), env.WORKSPACE)
            stash name: 'pipelines', includes: 'pipelines/**'
            echo("[Pipeline] calling  with application:" + application)
            echo("[Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
            utils.printColour("[Pipeline] calling with configuration:" + (new JsonBuilder(configurationContext).toPrettyString()), 'green')
        }


            nvdUpdateWorkflow(configurationContext, 'master')

            node('master') {
                archiveArtifacts artifacts: "pipelines/conf/configuration.json", onlyIfSuccessful: false // archive regardless of success
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
