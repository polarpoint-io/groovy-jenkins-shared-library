/*
 * Ian Purvis
 * InvokePerformance
 * Entry point for Performance Jobs
 * Called from the Jenkinsfile
 */

import io.polarpoint.workflow.ConfigurationContext
import groovy.json.JsonBuilder


echo("[Pipeline Performance] invoked")

def call(String application, String configuration, HashMap test_requirements) {
    ConfigurationContext configurationContext
    node('master') {
        checkout scm

        configurationContext = new ConfigurationContext(application, readFile(configuration), env.WORKSPACE)
        stash name: 'pipelines', includes: 'pipelines/**'
        echo("[Pipeline] calling  with application:" + application)
        echo("[Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
        echo("[Pipeline] calling  with configuration:" + ( new JsonBuilder( configurationContext ).toPrettyString()))


                performanceWorkflow(configurationContext,env.BRANCH_NAME,test_requirements)

            echo("[Pipeline] End.")
    }
}

return this
