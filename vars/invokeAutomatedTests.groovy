/*
 * Surj Bains  <surj@polarpoint.io>
 * Automated Tests Pipeline
 * Entry point for Automated Tests
 * Called from the Jenkinsfile
 */


import groovy.json.JsonBuilder
import io.polarpoint.workflow.ConfigurationContext

echo("[Automated Tests] invoked")
def scmVars

def call(String application, String configuration, HashMap test_requirements) {

    timestamps {
        // run this initially on the Jenkins master

        def utils = new io.polarpoint.utils.Utils()
        ConfigurationContext configurationContext
        node('master') {
            scmVars = checkout scm
            GitHubNotify(scmVars, 'Jenkins Build Pipeline', 'jenkinsci/jenkins-pipeline', 'PENDING')
            GitHubNotify(scmVars, 'Sonar Quality Gate', 'jenkinsci/sonar-quality', 'PENDING')



            configurationContext = new ConfigurationContext(application, readFile(configuration), env.WORKSPACE)
            stash name: 'pipelines', includes: 'pipelines/**'
            echo("[Automated Tests] calling  with application:" + application)
            echo("[Automated Tests] calling  with Branch:  $env.BRANCH_NAME ")
            utils.printColour("[Pipeline] calling with configuration:" + (new JsonBuilder(configurationContext).toPrettyString()), 'green')


            if (env.BRANCH_NAME =~ /^master/) {
                echo("[Automated Tests] master branch being build and tagged:" + application)
                automatedTestsWorkflow(configurationContext, 'master',test_requirements,scmVars)
            } else if (env.BRANCH_NAME =~ /^(development$|bugfix$)/) {
                echo("[Automated Tests] development or bugfix branch being build and tagged:" + application)
                automatedTestsWorkflow(configurationContext, env.BRANCH_NAME,test_requirements,scmVars)
            }  else {
                echo("[Automated Tests] not sure how to continue.")
            }
            echo("[Automated Tests] End.")

        }

        if (currentBuild.result == 'FAILURE') {
            GitHubNotify(scmVars, 'Jenkins Build Automated Tests Failed!', 'jenkinsci/jenkins-pipeline', 'FAILURE')
        } else {
            GitHubNotify(scmVars, 'Jenkins Build Automated Tests Succeeded!', 'jenkinsci/jenkins-pipeline', 'SUCCESS')
        }
    }

}

return this
