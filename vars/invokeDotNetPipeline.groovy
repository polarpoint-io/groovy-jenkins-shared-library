/*
 * Surj Bains  <surj@polarpoint.io>
 * InvokeDotNetWorkflow
 * Entry point for dot net Jobs
 * Called from the Jenkinsfile
 */
import groovy.json.JsonBuilder
import io.polarpoint.workflow.contexts.DotNetContext

echo("[Dot Net Pipeline] invoked")
def scmVars

def call(String application, String configuration) {

    timestamps {
        // run this initially on the Jenkins master

        def utils = new io.polarpoint.utils.Utils()
        DotNetContext dotNetContext
        node('master') {
            properties([disableConcurrentBuilds()])
            scmVars = checkout scm
            GitHubNotify(scmVars, 'Jenkins Build Pipeline', 'jenkinsci/jenkins-pipeline', 'PENDING')
            GitHubNotify(scmVars, 'Sonar Quality Gate', 'jenkinsci/sonar-quality', 'PENDING')


            dotNetContext = new DotNetContext(application, readFile(configuration), env.WORKSPACE)
            stash name: 'pipelines', includes: 'pipelines/**'
            echo("[Dot Net Pipeline]] calling  with application:" + application)
            echo("[Dot Net Pipeline]] calling  with Branch:  $env.BRANCH_NAME ")
            utils.printColour("[Dot Net Pipeline]] calling with configuration:" + (new JsonBuilder(dotNetContext).toPrettyString()), 'green')
        }

        if (env.BRANCH_NAME =~ /^master/) {
            echo("[Dot Net Pipeline]] master branch being built:" + application)
            dotNetWorkflow(dotNetContext, 'master', scmVars,true)
        } else if (env.BRANCH_NAME =~ /^(release$|development$|hotfix\/|bugfix\/|feature\/)/) {
            echo("[Dot Net Pipeline] development/release/hotfix/bugfix/feature branch being build and tagged:" + application)
            dotNetWorkflow(dotNetContext, env.BRANCH_NAME, scmVars, true)
        } else if (env.BRANCH_NAME =~ /(BH|PC|HD|SLR)-\d*/) {
            echo("[Dot Net Pipeline] feature  branch being built:" + application)
            dotNetWorkflow(dotNetContext, env.BRANCH_NAME, scmVars, false)

            node('master') {
                archiveArtifacts artifacts: "pipelines/conf/configuration.json", onlyIfSuccessful: false // archive regardless of success
            }
        } else {
            echo("[Dot Net Pipeline]] not sure how to continue.")
        }
        echo("[Dot Net Pipeline]] End.")


        if (currentBuild.result == 'FAILURE') {
            GitHubNotify(scmVars, 'Jenkins Build Pipeline Failed!', 'jenkinsci/jenkins-pipeline', 'FAILURE')
        } else {
            GitHubNotify(scmVars, 'Jenkins Build Pipeline Succeeded!', 'jenkinsci/jenkins-pipeline', 'SUCCESS')
        }
    }

}

return this
