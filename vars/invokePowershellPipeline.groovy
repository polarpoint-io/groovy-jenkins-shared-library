import groovy.json.JsonBuilder
import io.polarpoint.workflow.contexts.PowershellContext

echo("[Pipeline] invoked")
def scmVars

def call(String application, String configuration) {

    timestamps {
        // run this initially on the Jenkins master

        def utils = new io.polarpoint.utils.Utils()
        PowershellContext configurationContext
        node('master') {
            scmVars = checkout scm
            GitHubNotify(scmVars, 'Jenkins Build Pipeline', 'jenkinsci/jenkins-pipeline', 'PENDING')
            GitHubNotify(scmVars, 'Sonar Quality Gate', 'jenkinsci/sonar-quality', 'PENDING')

            configurationContext = new PowershellContext(application, readFile(configuration), env.WORKSPACE)
            stash name: 'pipelines', includes: 'pipelines/**'

            echo("[Powershell Pipeline] calling  with application:" + application)
            echo("[Powershell Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
            utils.printColour("[Pipeline] calling with configuration:" + (new JsonBuilder(configurationContext).toPrettyString()), 'green')
        }

        if (env.BRANCH_NAME =~ /^master/) {
            echo("[Powershell Pipeline] master branch being build and tagged:" + application)
            powershellWorkflow(configurationContext, 'master', scmVars,true)
        } else if (env.BRANCH_NAME =~ /^(development$|hotfix\/)/) {
            echo("[Powershell Pipeline] development or hotfix branch being build and tagged:" + application)
            powershellWorkflow(configurationContext, env.BRANCH_NAME, scmVars, true)
        } else if (env.BRANCH_NAME =~ /(BH|PC|HD|SLR)-\d*/) {
            echo("[Powershell Pipeline] feature  branch being built:" + application)
            powershellWorkflow(configurationContext, env.BRANCH_NAME, scmVars, false)
        } else {
            echo("[Powershell Pipeline] not sure how to continue.")
        }
        echo("[Powershell Pipeline] End.")


        if (currentBuild.result == 'FAILURE') {
            GitHubNotify(scmVars, 'Powershell Failed!', 'jenkinsci/jenkins-pipeline', 'FAILURE')
        } else {
            GitHubNotify(scmVars, 'Powershell Pipeline Succeeded!', 'jenkinsci/jenkins-pipeline', 'SUCCESS')
        }
    }

}

return this
