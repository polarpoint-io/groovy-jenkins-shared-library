/*
 * Surj Bains  <surj@polarpoint.io>
 * java_v_0_2_0_workflow.groovy
 * Entry point for all Java pipelines
 * Called from the Jenkinsfile
 */


import groovy.json.JsonBuilder
import io.polarpoint.workflow.contexts.Javav_0_2_0ConfigurationContext

echo("[Pipeline] invoked")
def scmVars

def call(String application, String configuration) {

    timestamps {
        // run this initially on the Jenkins master

        def utils = new io.polarpoint.utils.Utils()
        Javav_0_2_0ConfigurationContext configurationContext
        node('master') {
            properties([disableConcurrentBuilds()])
            scmVars = checkout scm
            GitHubNotify(scmVars, 'Jenkins Build Pipeline', 'jenkinsci/jenkins-pipeline', 'PENDING')
            GitHubNotify(scmVars, 'Sonar Quality Gate', 'jenkinsci/sonar-quality', 'PENDING')


            configurationContext = new Javav_0_2_0ConfigurationContext(application, readFile(configuration), env.WORKSPACE)
            stash name: 'pipelines', includes: 'pipelines/**'
            echo("[Pipeline] calling  with application:" + application)
            echo("[Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
            utils.printColour("[Pipeline] calling with configuration:" + (new JsonBuilder(configurationContext).toPrettyString()), 'green')
        }

        if (env.BRANCH_NAME =~ /^master/) {
            echo("[Pipeline] master branch being built:" + application)
            java_v_0_2_0_workflow(configurationContext, 'master', scmVars,true)
        } else if (env.BRANCH_NAME =~ /^(release$|development$|hotfix\/|bugfix\/|feature\/)/) {
            echo("[Pipeline] development/release/hotfix/bugfix/feature branch being build and tagged:" + application)
            java_v_0_2_0_workflow(configurationContext, env.BRANCH_NAME, scmVars, true)
        } else if (env.BRANCH_NAME =~ /(BH|PC|HD|SLR)-\d*/) {
            echo("[Pipeline] feature  branch being built:" + application)
            java_v_0_2_0_workflow(configurationContext, env.BRANCH_NAME, scmVars, false)

            node('master') {
                archiveArtifacts artifacts: "pipelines/conf/configuration.json", onlyIfSuccessful: false // archive regardless of success
                if (fileExists("Chart.yaml")) {
                    archiveArtifacts artifacts: "Chart.yaml", onlyIfSuccessful: false
                }
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
