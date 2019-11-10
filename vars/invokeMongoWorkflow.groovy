/*
 * Surj Bains  <surj@polarpoint.io>
 * InvokeMongoWorkflow
 * Entry point for Mongo client job
 * Called from the Jenkinsfile
 */


import groovy.json.JsonBuilder
import io.polarpoint.workflow.MongoContext

echo("[Mongo Pipeline] invoked")
def scmVars

def call(String application, String configuration) {

    def utils = new io.polarpoint.utils.Utils()
    MongoContext configurationContext
    node('master') {

        scmVars = checkout scm
        GitHubNotify(scmVars, 'Jenkins Build Pipeline', 'jenkinsci/jenkins-pipeline', 'PENDING')


        configurationContext = new MongoContext(application, readFile(configuration), env.WORKSPACE)
        stash name: 'pipelines', includes: 'pipelines/**'
        echo("[Mongo Pipeline] calling  with application:" + application)
        echo("[Mongo Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
        utils.printColour("[Pipeline] calling with configuration:" + (new JsonBuilder(configurationContext).toPrettyString()), 'green')
    }

    if (env.BRANCH_NAME =~ /^master/) {
        echo("[Mongo Pipeline] master branch being build and tagged:" + application)
        mongoWorkflow(configurationContext, 'master')
    } else if (env.BRANCH_NAME =~ /^(release$|development$|hotfix\/|bugfix\/|feature\/)/) {
        echo("[Mongo Pipeline] development branch being build and tagged:" + application)
        mongoWorkflow(configurationContext, env.BRANCH_NAME)
    } else if (env.BRANCH_NAME =~ /(PC|DAM|HD|SLR)-\d*/) {
        echo("[Mongo Pipeline] feature  branch being build and tagged:" + application)
        mongoWorkflow(configurationContext, env.BRANCH_NAME)

    } else {
        echo("[Mongo Pipeline] not sure how to continue.")
    }
    echo("[Pipeline] End.")

    if (currentBuild.result == 'FAILURE') {
        GitHubNotify(scmVars, 'Jenkins Build Pipeline Failed!', 'jenkinsci/jenkins-pipeline', 'FAILURE')
    } else {
        GitHubNotify(scmVars, 'Jenkins Build Pipeline Succeeded!', 'jenkinsci/jenkins-pipeline', 'SUCCESS')
    }


}


return this
