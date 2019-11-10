/*
 * Surj Bains  <surj@polarpoint.io>
 * InvokeTerraformPipeline
 * Entry point for all Jobs
 * Called from the Jenkinsfile
 */


import groovy.json.JsonBuilder
import io.polarpoint.workflow.contexts.TerraformContext

echo("[Terraform Pipeline] invoked")
def scmVars

def call(String application, String configuration) {

    timestamps {
        // run this initially on the Jenkins master

        def utils = new io.polarpoint.utils.Utils()
        TerraformContext terraformContext
        node('master') {
            properties([disableConcurrentBuilds()])
            scmVars = checkout scm
            GitHubNotify(scmVars, 'Jenkins Build Pipeline', 'jenkinsci/jenkins-pipeline', 'PENDING')
            GitHubNotify(scmVars, 'Sonar Quality Gate', 'jenkinsci/sonar-quality', 'PENDING')


            terraformContext = new TerraformContext(application, readFile(configuration), env.WORKSPACE)
            stash name: 'pipelines', includes: 'pipelines/**'
            echo("[Terraform Pipeline] calling  with application:" + application)
            echo("[Terraform Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
            utils.printColour("[Pipeline] calling with configuration:" + (new JsonBuilder(terraformContext).toPrettyString()), 'green')
        }

        if (env.BRANCH_NAME =~ /^master/) {
            echo("[Terraform Pipeline] master branch being build and tagged:" + application)
            terraformWorkflow(terraformContext, 'master', scmVars,true)
        } else if (env.BRANCH_NAME =~ /^(development$|hotfix\/)/) {
            echo("[Terraform Pipeline] development or hotfix branch being build and tagged:" + application)
            terraformWorkflow(terraformContext, env.BRANCH_NAME, scmVars, true)
        } else if (env.BRANCH_NAME =~ /(BH|PC|HD|SLR)-\d*/) {
            echo("[Terraform Pipeline] feature  branch being built:" + application)
            terraformWorkflow(terraformContext, env.BRANCH_NAME, scmVars, false)
        } else {
            echo("[Terraform Pipeline] not sure how to continue.")
        }
        echo("[Terraform Pipeline] End.")


        if (currentBuild.result == 'FAILURE') {
            GitHubNotify(scmVars, 'Jenkins Terraform Pipeline Failed!', 'jenkinsci/jenkins-pipeline', 'FAILURE')
        } else {
            GitHubNotify(scmVars, 'Jenkins Terraform Pipeline Succeeded!', 'jenkinsci/jenkins-pipeline', 'SUCCESS')
        }
    }

}

return this
