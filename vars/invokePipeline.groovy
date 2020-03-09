/*
 * Surj Bains  <surj@polarpoint.io>
 * InvokePipeline
 * Entry point for all Jobs
 * Called from the Jenkinsfile
 */


import groovy.json.JsonBuilder
import io.polarpoint.workflow.ConfigurationContext

echo("[Pipeline] invoked")
def scmVars

def call(String application, String configuration) {

    timestamps {
        // Env variable to identify if there haven't been new commits since last build
        env.no_new_commits = false

        def Boolean isBugfixOrHotfix = false

        // run this initially on the Jenkins master

        def utils = new io.polarpoint.utils.Utils()
        ConfigurationContext configurationContext
        node('master') {
            properties([disableConcurrentBuilds()])
            scmVars = checkout scm
            GitHubNotify(scmVars, 'Jenkins Build Pipeline', 'jenkinsci/jenkins-pipeline', 'PENDING')
            GitHubNotify(scmVars, 'Sonar Quality Gate', 'jenkinsci/sonar-quality', 'PENDING')


            configurationContext = new ConfigurationContext(application, readFile(configuration), env.WORKSPACE)
            stash name: 'pipelines', includes: 'pipelines/**'
            echo("[Pipeline] calling  with application:" + application)
            echo("[Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
            utils.printColour("[Pipeline] calling with configuration:" + (new JsonBuilder(configurationContext).toPrettyString()), 'green')
        }

        if (env.BRANCH_NAME =~ /^master/) {
            echo("[Pipeline] master branch being built:" + application)
            javaWorkflow(configurationContext, 'master', scmVars,true)
        } else if (env.BRANCH_NAME =~ /^(release$|development$|hotfix\/|bugfix\/|feature\/)/) {
            echo("[Pipeline] development/release/hotfix/bugfix/feature branch being build and tagged:" + application)
            javaWorkflow(configurationContext, env.BRANCH_NAME, scmVars, true)

            isBugfixOrHotfix = (env.BRANCH_NAME =~ /^(hotfix\/|bugfix\/)/)
            echo "isBugfixOrHotfix: ${isBugfixOrHotfix}"

        } else if (env.BRANCH_NAME =~ /(BH|PC|HD|SLR)-\d*/) {
            echo("[Pipeline] feature  branch being built:" + application)
            javaWorkflow(configurationContext, env.BRANCH_NAME, scmVars, false)

            node('master') {
                archiveArtifacts artifacts: "pipelines/conf/configuration.json", onlyIfSuccessful: false // archive regardless of success
                if (fileExists("Chart.yaml")) {
                    archiveArtifacts artifacts: "Chart.yaml", onlyIfSuccessful: false
                }
            }
        } else {
            echo("[Pipeline] not sure how to continue.")
        }
        echo("[Pipeline] End."+currentBuild.result)

        if(isBugfixOrHotfix) {
            node('master') {
                echo "Check out again to ensure notify is made to the latest commit"
                // Check out again to ensure notify is made to the latest commit
                scmVars = checkout scm
            }
        }

        if (currentBuild.result == 'FAILURE') {
            GitHubNotify(scmVars, 'Jenkins Build Pipeline Failed!', 'jenkinsci/jenkins-pipeline', 'FAILURE')
        } else {
            try {
                GitHubNotify(scmVars, 'Jenkins Build Pipeline Succeeded!', 'jenkinsci/jenkins-pipeline', 'SUCCESS')
            }
            catch(err)
            {

                echo err.message+'Eror in Github API Notify'
            }

        }
    }

}

return this
