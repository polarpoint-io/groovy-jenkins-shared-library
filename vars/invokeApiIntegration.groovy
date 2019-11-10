/*
 * Surj Bains  <surj@polarpoint.io>
 * apiIntegrationWorkflow
 * for E2E integration testing (bdd)
 * Called from the Jenkinsfile
 */


import groovy.json.JsonBuilder
import io.polarpoint.workflow.ApiIntegrationContext

echo("[API Integration] invoked")

def call(String application, String configuration, String git_branch, HashMap test_requirements) {
    env.BRANCH_NAME = git_branch
    def apiContext

    timeout(time: 8, unit: 'HOURS') {
        node('master') {
            checkout scm
            apiContext = new ApiIntegrationContext(application, readFile(configuration), env.WORKSPACE)
            stash name: 'pipelines', includes: 'pipelines/**'
            echo("[API Integration] calling  with application:" + application)
            echo("[API Integration] calling  with Branch:  $env.BRANCH_NAME ")
            echo("[API Integration] calling  with configuration:" + (new JsonBuilder(apiContext).toPrettyString()))
        }

        // Fix the Reg Expression for HD Branch Name parmarv
        if (env.BRANCH_NAME =~ /^master/) {
            echo("[API Integration Integration] master branch being run:" + application)
            apiIntegrationWorkflow(apiContext, 'master', test_requirements)
        } else if (env.BRANCH_NAME =~ /(^development)/) {
            echo("[API Integration Integration]  development branch being run:" + application)
            apiIntegrationWorkflow(apiContext, 'development', test_requirements)
        } else if (env.BRANCH_NAME =~ /(PC|HD|SLR|DAM)-\d*/) {
            echo("[API Integration Integration]  feature branch being run:" + application)
            apiIntegrationWorkflow(apiContext, env.BRANCH_NAME, test_requirements)
        } else if (env.BRANCH_NAME =~ /^preprod/) {
            echo("[API Integration Integration]  preprod branch being run:" + application)
            apiIntegrationWorkflow(apiContext, 'preprod', test_requirements)
        } else {
            echo("[Pipeline] not sure how to continue with "+env.BRANCH_NAME)
        }
        echo("[Pipeline] End.")
    }
}


return this
