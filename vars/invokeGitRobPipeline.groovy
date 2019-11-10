/*
 * Surj Bains  <surj@polarpoint.io>
 * InvokeGitRobPipeline
 * Entry point for GitRob Job
 * Called from the Jenkinsfile
 */


import groovy.json.JsonBuilder
import io.polarpoint.workflow.contexts.GitRobContext

echo("[Pipeline] invoked")
def scmVars

def call(String application, String configuration) {

    timestamps {
        // run this initially on the Jenkins master

        def utils = new io.polarpoint.utils.Utils()
        GitRobContext configurationContext
        node('master') {
            properties([disableConcurrentBuilds()])
            scmVars = checkout scm
            GitHubNotify(scmVars, 'Jenkins Build Pipeline', 'jenkinsci/jenkins-pipeline', 'PENDING')
            GitHubNotify(scmVars, 'Sonar Quality Gate', 'jenkinsci/sonar-quality', 'PENDING')


            configurationContext = new GitRobContext(application, readFile(configuration), env.WORKSPACE)
            stash name: 'pipelines', includes: 'pipelines/**'
            echo("[Pipeline] calling  with application:" + application)
            echo("[Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
            utils.printColour("[Pipeline] calling with configuration:" + (new JsonBuilder(configurationContext).toPrettyString()), 'green')
        }

        if (env.BRANCH_NAME =~ /^master/) {
            echo("[Pipeline] master branch being built:" + application)
            gitRobWorkflow(configurationContext, 'master')
        } else if (env.BRANCH_NAME =~ /^(release$|development$|hotfix\/|bugfix\/|feature\/)/) {
            echo("[Pipeline] development/release/hotfix/bugfix/feture branch being build and tagged:" + application)
            gitRobWorkflow(configurationContext, env.BRANCH_NAME)
        } else if (env.BRANCH_NAME =~ /(BH|PC|HD|SLR)-\d*/) {
            echo("[Pipeline] feature  branch being built:" + application)
            gitRobWorkflow(configurationContext, env.BRANCH_NAME)

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
