/*
 * invokePrometheus cofig 
 * Called from the Jenkinsfile
 */


import groovy.json.JsonBuilder
import io.polarpoint.workflow.contexts.PrometheusConfigContext


def scmVars

def call(String application, String configuration) {
    echo("[Prometheus alert Manager  Pipeline] invoked")

    timestamps {
        // run this initially on the Jenkins master
        def utils = new io.polarpoint.utils.Utils()
        PrometheusConfigContext PrometheusContext
        node('master') {
            properties([disableConcurrentBuilds()])
            scmVars = checkout scm
            GitHubNotify(scmVars, 'Jenkins Build Pipeline', 'jenkinsci/jenkins-pipeline', 'PENDING')
            GitHubNotify(scmVars, 'Sonar Quality Gate', 'jenkinsci/sonar-quality', 'PENDING')


            PrometheusContext = new PrometheusConfigContext(application, readFile(configuration), env.WORKSPACE)
            stash name: 'pipelines', includes: 'pipelines/**'
            echo("[Prometheus alert Manager Pipeline] calling  with application:" + application)
            echo("[Prometheus alert Manager Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
            utils.printColour("[Pipeline] calling with configuration:" + (new JsonBuilder(PrometheusContext).toPrettyString()), 'green')
        }

        if (env.BRANCH_NAME =~ /^master/) {
            echo("[Prometheus alert Manager Pipeline] master branch being build and tagged:" + application)
            prometheusConfigWorkflow(PrometheusContext, 'master', scmVars,true)
        } else if (env.BRANCH_NAME =~ /^(development$|hotfix\/)/) {
            echo("[Prometheus alert Manager Pipeline] development or hotfix branch being build and tagged:" + application)
            prometheusConfigWorkflow(PrometheusContext, env.BRANCH_NAME, scmVars, true)
        } else if (env.BRANCH_NAME =~ /(BH|PC|HD|SLR)-\d*/) {
            echo("[Prometheus alert Manager Pipeline] feature  branch being built:" + application)
            prometheusConfigWorkflow(PrometheusContext, env.BRANCH_NAME, scmVars, false)
        } else {
            echo("[Prometheus alert Manager Pipeline] not sure how to continue.")
        }
        echo("[Prometheus alert Manager Pipeline] End.")


        if (currentBuild.result == 'FAILURE') {
            GitHubNotify(scmVars, 'Prometheus alert Manager Failed!', 'jenkinsci/jenkins-pipeline', 'FAILURE')
        } else {
            GitHubNotify(scmVars, 'Prometheus alert Manager Pipeline Succeeded!', 'jenkinsci/jenkins-pipeline', 'SUCCESS')
        }
    }

}

return this
