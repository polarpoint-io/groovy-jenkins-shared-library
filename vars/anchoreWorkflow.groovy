/*
 * Surj Bains  <surj@polarpoint.io>
 * genericWorkflow used to test, package, tag and deploy artifacts
 */


import io.polarpoint.workflow.AnchoreContext
import io.polarpoint.utils.Slack

def call(AnchoreContext context, String targetBranch) {
    def genericHandlers = context.getConfigurableAnchoreHandlers()

    def qualityTests = []
    def success = false
    def label = "jnlp"
    def Slack  = new Slack(this)


    String targetEnvironment = "${context.application}-${targetBranch}"


    timestamps {
        try {


            node('master') {
                Slack.sender(true, [buildStatus: 'STARTED'])
                echo("target_branch:" + targetBranch)
                checkout scm

                echo("load quality tests..." + genericHandlers.qualityTests)
                for (String test : genericHandlers.qualityTests) {
                    echo("load quality tests:" + test)
                    qualityTests.add(load("${test}"))
                }

                node('curljq') {
                    sh "id"
                    checkout scm
                    Slack.sender(true, [buildStatus: 'PROGRESS'])
                    container('curljq') {
                        milestone(label: 'Docker Image Running Anchore Build')

                        stage("Anchore scan") {
                            def integrationSchedule = [:]
                            for (Object testClass : qualityTests) {
                                def currentTest = testClass
                                integrationSchedule[currentTest.name()] = {
                                    currentTest.runTest(targetBranch, context)
                                }
                            }
                            try {
                                parallel integrationSchedule
                                milestone(label: 'Anchore scan')
                            } catch (err) {
                                err.printStackTrace()
                                echo err.message
                                echo("Anchore scan")
                                throw err
                            } finally {
                                // Notify----------------------------//
                                stage("Notify") {
                                    try {
                                        if (success) {
                                            slackSend(color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
                                        } else {
                                            slackSend(color: '#FF0000', message: "VULNERABILITES FOUND AT: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
                                            echo "There are some new vulnerabilities found during anchore scanning stage."
                                        }
                                    } catch (err) {
                                        println err.message
                                        error "Notifications failed."
                                    } finally {
                                        // do nothing
                                    }
                                }
                            }
                        }
                    }
                }
            }
            success = true
        }

        catch (err) {

            echo err.message
            error("Pipeline tasks have failed.")

        } finally {
            // Notify----------------------------//
            stage("Notify") {
                try {

                    if (success) {
                        def config = [
                                buildStatus: 'SUCCESS'
                        ];
                        Slack.sender(config)

                    } else {
                        def config = [
                                buildStatus: 'FAILURE'
                        ];
                        Slack.sender(config)

                        echo "Pipeline tasks have failed."
                    }

                } catch (err) {
                    println err.message
                    error "Notifications failed."
                } finally {

                    // do nothing
                }
            }
        }
    }
}