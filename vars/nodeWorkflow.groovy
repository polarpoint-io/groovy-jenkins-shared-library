import io.polarpoint.workflow.NodeContext
import io.polarpoint.utils.Slack


def call(NodeContext context, String targetBranch) {
    def stageHandlers = context.configurableNodeHandlers

    def deployer
    def publisher
    def containerScanner
    def containerStager
    def unitTests = []
    def Slack  = new Slack(this)

    def success = false
    def targetEnvironment = "${context.application}-${targetBranch}"

    node('master') {

        Slack.sender(true, [ buildStatus: 'STARTED' ])

        echo("target_branch:" + targetBranch)
        checkout scm

        echo("load unit tests..." + stageHandlers.unitTests)
        for (String test : stageHandlers.unitTests) {
            echo("load quality tests:" + test)
            unitTests.add(load("${test}"))
        }

        echo("Loading publisher: " + stageHandlers.publisher)
        publisher = load(stageHandlers.publisher)

        echo("Loading deployer: " + stageHandlers.deployer)
        deployer = load(stageHandlers.deployer)

        echo("Loading container builder: " + stageHandlers.containerBuilder)
        containerBuilder = load(stageHandlers.containerBuilder)

        echo("Loading container stager: " + stageHandlers.containerStager)
        containerStager = load(stageHandlers.containerStager)

        echo("Loading container scanner: " + stageHandlers.containerScanner)
        containerScanner = load(stageHandlers.containerScanner)

        echo("Loading container publisher: " + stageHandlers.containerPublisher)
        containerPublisher = load(stageHandlers.containerPublisher)
    }

    try {

        Slack.sender(true, [ buildStatus: 'PROGRESS' ])

        podTemplate ( label: "nodejs" ) {
            node('nodejs') {
                container('nodejs') {
                    checkout scm

                    milestone(label: 'Unit Tests')
                    stage("Unit Tests") {
                        for (Object testClass : unitTests) {
                            def currentTest = testClass
                            currentTest.runTest(targetBranch, context)
                        }
                    }

                    Slack.sender(true, [ buildStatus: 'PROGRESS' ])

                    milestone(label: 'Tag')
                    stage("Tag") {
                        invokeSemanticVersioning(targetBranch, context)
                    }

                    milestone(label: 'Publish')
                    stage("Publish") {
                        publisher.publish(targetBranch, context)
                    }
                }
            }
        }

        podTemplate(label: "docker") {
            node('docker') {

                milestone(label: 'Docker Build')
                stage("Docker Build") {
                    container('docker') {
                        containerBuilder.build(targetBranch, context)
                    }
                }

                milestone(label: 'Staging Docker Registry Publish')
                stage("Staging Docker Registry Publish") {

                    container('docker') {
                        containerStager.stage(targetBranch, context)
                    }

                }

                milestone(label: 'Docker Container Scanner')
                stage("Docker Container Scanner") {

                    container('docker') {
                        containerScanner.scan(targetBranch, context)
                    }

                }

                milestone(label: 'Docker Registry Publish')
                stage("Docker Registry Publish") {
                    container('docker') {
                        containerPublisher.publish(targetBranch, context)
                    }
                }
            }
        }

        success = true

    } catch (err) {

        echo err.message
        error("Pipeline tasks have failed.")

    } finally {

        // Notify----------------------------//
        stage("Notify") {
            try {
                if (success) {
                    // Update confluence with build results
                    //invokeConfluence(targetBranch, context, 'SUCCESS')
                    def config = [
                            buildStatus: 'SUCCESS'
                    ];
                    Slack.sender(config)
                    // slackSend(color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")


                } else {
                    // Update confluence with build results
                    //invokeConfluence(targetBranch, context, 'FAILURE')
                    def config = [
                            buildStatus: 'FAILURE'
                    ];
                    Slack.sender(config)
                    //slackSend(color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")

                    echo "Pipeline tasks have failed."
                }

            } catch (err) {

                echo err.message
                error("Notifications failed.")
            } finally {

                // do nothing
            }
        }
    }
}

return this
