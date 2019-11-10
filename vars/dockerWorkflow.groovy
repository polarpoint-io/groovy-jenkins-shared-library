/*
 * Surj Bains  <surj@polarpoint.io>
 * dockerWorkflow used to test, package, tag and deploy Docker images
 */


import io.polarpoint.workflow.DockerContext
import io.polarpoint.utils.Slack

def call(DockerContext context, String targetBranch) {
    def dockerHandlers = context.getDockerHandlers()
    def builder
    def scanner
    def publisher
    def stager
    def qualityTests = []
    def success = false
    def label = "docker"
    def appContainer
    def Slack  = new Slack(this)

    String targetEnvironment = "${context.application}-${targetBranch}"
    timestamps {


//        echo("load quality tests..." + dockerHandlers.qualityTests)
//        for (String test : dockerHandlers.qualityTests) {
//            echo("load quality tests:" + test)
//            qualityTests.add(load("${test}"))
//        }
        echo("Loading builder: " + dockerHandlers.builder)
        builder = load(dockerHandlers.builder)

        echo("Loading stager: " + dockerHandlers.stager)
        stager = load(dockerHandlers.stager)

        echo("Loading scanner: " + dockerHandlers.scanner)
        scanner = load(dockerHandlers.scanner)

        echo("Loading publisher: " + dockerHandlers.publisher)
        publisher = load(dockerHandlers.publisher)


        podTemplate(label: label) {
            node("jnlp") {
                Slack.sender(true, [ buildStatus: 'STARTED' ])
                sh "id"
                checkout scm

                try {

                    podTemplate(label: "docker") {
                        node('docker') {
                            milestone(label: 'Docker Build')
                            stage("Docker Build") {

                                container('docker') {
                                    builder.build(targetBranch, context)
                                }

                            }
                        }
                    }

                    podTemplate(label: "docker") {
                        node('docker') {
                            milestone(label: 'Staging Docker Registry Publish')
                            stage("Staging Docker Registry Publish") {

                                container('docker') {
                                    stager.stage(targetBranch, context)
                                }

                            }

                            milestone(label: 'Docker Container Scanner')
                            stage("Docker Container Scanner") {

                                container('docker') {
                                    scanner.scan(targetBranch, context)
                                }

                            }

//                            podTemplate(label: "jnlp") {
//                                milestone(label: 'Tag')
//                                stage("Tag") {
//                                    container('gradle') {
//                                        invokeSemanticVersioning(targetBranch, context)
//                                    }
//                                }
//                            }

                            milestone(label: 'Docker Registry Publish')
                            stage("Docker Registry Publish") {

                                container('docker') {
                                    publisher.publish(targetBranch, context)
                                }

                            }
                        }
                    }



                    success = true

                } catch (error) {

                    echo error.message
                    echo("Pipeline tasks have failed.")
                    throw error
                } finally {

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
        }
    }
}