/*
 * Surj Bains  <surj@polarpoint.io>
 * genericWorkflow used to test, package, tag and deploy artifacts
 */


import io.polarpoint.workflow.GenericContext

def call(GenericContext context, String targetBranch) {
    def genericHandlers = context.getConfigurableGenericHandlers()
    def builder
    def deployer
    def publisher
    def qualityTests = []
    def success = false
    def label = "jnlp"

    String targetEnvironment = "${context.application}-${targetBranch}"
    timestamps {

        node('master') {
            echo("target_branch:" + targetBranch)
            checkout scm

            echo("load quality tests..." + genericHandlers.qualityTests)
            for (String test : genericHandlers.qualityTests) {
                echo("load quality tests:" + test)
                qualityTests.add(load("${test}"))
            }
            echo("Loading builder: " + genericHandlers.builder)
            builder = load(genericHandlers.builder)

            echo("Loading publisher: " + genericHandlers.publisher)
            publisher = load(genericHandlers.publisher)

            echo("Loading deploy: " + genericHandlers.deployer)
            deployer = load(genericHandlers.deployer)
        }

        try {
            milestone(label: 'Publish')
            podTemplate(label: 'maven') {
                node('maven') {
                    checkout scm
                    milestone(label: 'Quality Tests')
                    container('maven') {

                        stage("Quality Tests") {
                            for (Object testClass : qualityTests) {
                                def currentTest = testClass
                                currentTest.runTest(targetBranch, context)
                            }
                        }


                        milestone(label: 'Build')
                        stage("Build") {
                            builder.build(targetBranch, context)
                        }


                        stage("Publish") {
                            publisher.publish(targetBranch, context)
                        }


                        milestone(label: 'Deploy')
                        // Locked Deploy Cycle-----------------//
                        lock(inversePrecedence: true, quantity: 1, resource: targetEnvironment) {
                            stage("Deploy") {
                                if (deployer != null) {

                                    deployer.deploy(targetBranch, context)

                                } else {
                                    echo("nothing to deploy")
                                }
                            }

                            success = true

                        }
                    }
                    cleanWs()
                }

            }
        } catch (err) {

            err.printStackTrace()
            error("Pipeline tasks have failed.")

        } finally {

            // Notify----------------------------//
            stage("Notify") {
                try {
                    if (success) {
                        slackSend(color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
                        currentBuild.result = 'SUCCESS'

                    } else {
                        slackSend(color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
                        currentBuild.result = 'FAILURE'

                        echo "Pipeline tasks have failed."
                    }

                } catch (err) {
                    err.printStackTrace()
                    echo err.message
                    error("Notify failed failed")


                } finally {

                    // do nothing
                }
            }
        }
    }
}