/*
 * Surj Bains  <surj@polarpoint.io>
 * integration Pipeline
 * This Pipeline is used to promote artifacts to
 * higher environments and initiate integration tests
 */


import io.polarpoint.workflow.ConfigurationContext
import io.polarpoint.workflow.Utils



def call(ConfigurationContext context, String targetBranch) {
    def stageHandlers = context.getConfigurableStageHandlers()
    def deployer
    def integrationTests = []
    def allTests = []



    def success = false


    String targetEnvironment = "${context.application}-${targetBranch}"

    // run this initially on the Jenkins master
    node('master') {
        echo("target_branch:" + targetBranch)
        checkout scm

//        echo("Loading deployer: " + stageHandlers.deployer)
//        deployer = load(stageHandlers.deployer)

        echo("load integration tests..." + stageHandlers.integrationTests)
        for (String test : stageHandlers.integrationTests) {
            echo("load integration tests:" + test)
            integrationTests.add(load("${test}"))
        }

        allTests.addAll(integrationTests)

    }

    milestone(label: 'Deploy')
    try {
    // Locked Deploy Cycle-----------------//
//    lock(inversePrecedence: true, quantity: 1, resource: targetEnvironment) {
//        stage("Deploy") {
//            deployer.deploy(targetBranch, context)
//        }
//
//    }

        milestone(label: 'Integration Tests')
        stage("Integration tests") {
            def integrationSchedule = [:]
            for (Object testClass : integrationTests) {
                def currentTest = testClass
                integrationSchedule[currentTest.name()] = { currentTest.runTest(targetBranch, context) }
            }
            try {
                parallel integrationSchedule
                milestone(label: 'Integration tests')
            } catch (error) {
                error.printStackTrace()
                echo error.message
                echo("Integration tests failed")
                throw error
            } finally {
                //
            }
        }
        success = true

    } catch (error) {
        echo error.message
        echo("some mandatory steps have failed, Aborting ")
        throw error
    } finally {

        // Notify----------------------------//
        stage("Notify") {
            try {
                if (success) {

                    // Update confluence with build results
                    invokeConfluence(targetBranch, context, 'SUCCESS')
                    slackSend(color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")


                } else {
                    // Update confluence with build results
                    invokeConfluence(targetBranch, context, 'FAILURE')
                    slackSend(color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")

                    echo "Pipeline tasks have failed."
                }

            } catch (error) {
                echo error.message
                //echo Utils.stackTrace(error)
                echo "Notifications failed."
            } finally {

            }

        }
    }
}
return this;
