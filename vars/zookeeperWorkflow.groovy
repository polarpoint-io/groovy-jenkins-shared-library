/*
 * Surj Bains  <surj@polarpoint.io>
 * buildWorkflow
 * This Pipeline is used to create zookeeper artifacts to be deployed to environments
 */


import io.polarpoint.workflow.ZookeeperContext
import io.polarpoint.utils.Slack

def call(ZookeeperContext context, String targetBranch) {
    def stageHandlers = context.configurableNodeHandlers

    def deployer
    def publisher
    def unitTests = []
    boolean success = false
    def label = "jnlp"
    def Slack  = new Slack(this)

    String targetEnvironment = "${context.application}-${targetBranch}"

    node('master') {
        Slack.sender(true, [ buildStatus: 'STARTED' ])

        checkout scm
        echo("target_branch:" + targetBranch)

        echo("Load unit tests..." + stageHandlers.unitTests)
        for (String test : stageHandlers.unitTests) {
            echo("load unit tests:" + test)
            unitTests.add(load("${test}"))
        }

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
        Slack.sender(true, [ buildStatus: 'PROGRESS' ])
        podTemplate(label: label) {
            node(label) {
                cleanWs()
                sh "id"
                container('jnlp') {

                    checkout scm

                    milestone(label: 'Unit Tests')
                    stage("Unit Tests") {
                        for (Object testClass : unitTests) {
                            def currentTest = testClass
                            currentTest.runTest(targetBranch, context)
                        }
                    }

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

                }
                cleanWs()
            }
        }

        milestone(label: 'Deploy')
        // Locked Deploy Cycle-----------------//
        lock(inversePrecedence: true, quantity: 1, resource: targetEnvironment) {
            node('master') {
                stage("Deploy") {
                    if (deployer != null) {
                        deployer.deploy(targetBranch, context)

                    } else {
                        echo("nothing to deploy")
                    }

                }
                cleanWs()
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


    } catch (err) {

        echo err.message
        echo("Pipeline tasks have failed.")

    } finally {

        // Notify----------------------------//
        stage("Notify") {
            try {

                if (success) {

                    def config = [
                            buildStatus: 'SUCCESS'
                    ];
                    Slack.sender(config)
                    currentBuild.result = 'SUCCESS'

                } else {
                    def config = [
                            buildStatus: 'FAILURE'
                    ];
                    Slack.sender(config)
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