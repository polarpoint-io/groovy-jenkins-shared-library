/*
 * Surj Bains  <surj@polarpoint.io>
 * InvokeZookeeperWorkflow
 * Entry point for Zookeeper client job
 * Called from the Jenkinsfile
 */


import groovy.json.JsonBuilder
import io.polarpoint.workflow.ZookeeperContext

echo("[Zookeeper Pipeline] invoked")
def scmVars

def call(String application, String configuration) {

    def utils = new io.polarpoint.utils.Utils()
    ZookeeperContext configurationContext
    node('master') {

        scmVars = checkout scm
        GitHubNotify(scmVars, 'Jenkins Build Pipeline', 'jenkinsci/jenkins-pipeline', 'PENDING')


        configurationContext = new ZookeeperContext(application, readFile(configuration), env.WORKSPACE)
        stash name: 'pipelines', includes: 'pipelines/**'
        echo("[Zookeeper Pipeline] calling  with application:" + application)
        echo("[Zookeeper Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
        utils.printColour("[Pipeline] calling with configuration:" + (new JsonBuilder(configurationContext).toPrettyString()), 'green')
    }

    if (env.BRANCH_NAME =~ /^master/) {
        echo("[Zookeeper Pipeline] master branch being build and tagged:" + application)
        zookeeperWorkflow(configurationContext, 'master')
    } else if (env.BRANCH_NAME =~ /^(release$|development$|hotfix\/)/) {
        echo("[Zookeeper Pipeline] development branch being build and tagged:" + application)
        zookeeperWorkflow(configurationContext, env.BRANCH_NAME)
    } else if (env.BRANCH_NAME =~ /(PC|HD|SLR)-\d*/) {
        echo("[Zookeeper Pipeline] feature  branch being build and tagged:" + application)
        zookeeperWorkflow(configurationContext, env.BRANCH_NAME)

    } else {
        echo("[Zookeeper Pipeline] not sure how to continue.")
    }
    echo("[Pipeline] End.")

    if (currentBuild.result == 'FAILURE') {
        GitHubNotify(scmVars, 'Jenkins Build Pipeline Failed!', 'jenkinsci/jenkins-pipeline', 'FAILURE')
    } else {
        GitHubNotify(scmVars, 'Jenkins Build Pipeline Succeeded!', 'jenkinsci/jenkins-pipeline', 'SUCCESS')
    }


}


return this
