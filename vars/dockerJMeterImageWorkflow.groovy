/*
 * Surj Bains  <surj@polarpoint.io>
 * genericWorkflow used to test, package, tag and deploy Docker images
 */


import io.polarpoint.workflow.DockerImageContext

def call(DockerImageContext context, String targetBranch,HashMap test_requirements) {
    def dockerHandlers = context.getConfigurableImageHandlers()
    def containerrun
    def success = false
    def label = "docker"
    def appContainer
    def integrationTests = []
    def allTests = []

    String targetEnvironment = "${context.application}-${targetBranch}"
    timestamps {


            echo("Loading Containerize jmeter in METERRY master node :::: " + dockerHandlers.containerrun)

            echo("load integration tests..." + dockerHandlers.integrationTests)
            for (String test : dockerHandlers.integrationTests) {
                echo("load integration tests:::" + test)
                integrationTests.add(load("${test}"))
            }

            allTests.addAll(integrationTests)


        containerrun = load(dockerHandlers.containerrun)

        podTemplate(label: "jmeter") {

            try {

            node('jmeter') {
                sh "id"
                checkout scm

                                    container('docker-perf-jmeter') {
                                        milestone(label: 'Docker Image Runing jmeter Build')

                                         stage("Performance Jmeter tests") {
                                            def integrationSchedule = [:]
                                            for (Object testClass : integrationTests) {
                                                def currentTest = testClass
                                                integrationSchedule[currentTest.name()] = {
                                                    currentTest.runTest(targetBranch, context, test_requirements)
                                                }
                                            }
                                            try {
                                                parallel integrationSchedule
                                                milestone(label: 'automated jmeter tests')
                                            } catch (err) {
                                                err.printStackTrace()
                                                echo err.message
                                                echo("automated tests failed")
                                                throw err
                                            } finally {
                                                //
                                            }
                                        }


                                        echo("runnning new container " )
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

    }
}