/*
 * Surj Bains  <surj@polarpoint.io>
 * buildWorkflow
 * This Pipeline is used to create mongo artifacts to be deployed to environments
 */


import io.polarpoint.workflow.MongoContext

def call(MongoContext context, String targetBranch) {
    def stageHandlers = context.configurableNodeHandlers

    def deployer
    def publisher
    def unitTests
    def success = false


    String targetEnvironment = "${context.application}-${targetBranch}"

    node('master') {
        echo("target_branch:" + targetBranch)
        checkout scm

        echo("Loading builder: " + stageHandlers.builder)
        builder = load(stageHandlers.builder)

        echo("Loading publisher: " + stageHandlers.publisher)
        publisher = load(stageHandlers.publisher)

        echo("Loading deploy: " + stageHandlers.deployer)
        deployer = load(stageHandlers.deployer)

        echo("Loading container builder: " + stageHandlers.containerBuilder)
        containerBuilder = load(stageHandlers.containerBuilder)

        echo("Loading container publisher: " + stageHandlers.containerPublisher)
        containerPublisher = load(stageHandlers.containerPublisher)

    }

    try {

        podTemplate(label: "mongo-cli") {

            node('mongo-cli') {

                container('mongo-cli') {


                    checkout scm

                    milestone(label: 'Build')
                    stage("Build") {
                        builder.build(targetBranch, context)
                    }

                    milestone(label: 'Tag')
                    stage("Tag") {
                        invokeSemanticVersioning(targetBranch, context)
                    }

                    milestone(label: 'Publish')
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
                    }
                    cleanWs()
                }
            }
        }

        podTemplate(label: "docker") {
            node('docker') {
                checkout scm

                milestone(label: 'Docker Build')
                stage("Docker Build") {
                    container('docker') {
                        containerBuilder.build(targetBranch, context)
                    }
                }

                milestone(label: 'Docker Registry Publish')
                stage("Docker Registry Publish") {

                    container('docker') {
                        containerPublisher.publish(targetBranch, context)
                    }
                }
                cleanWs()
            }
        }

        success = true

    } catch (error) {

        echo error.message
        echo("Pipeline tasks have failed.")
        throw error
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

            } catch (error) {
                echo error.message
                //echo Utils.stackTrace(error)
                echo "Notifications failed."
            } finally {

                // do nothing
            }
        }
    }
}


return this;