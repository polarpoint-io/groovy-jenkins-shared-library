/*
 * Surj Bains  <surj@polarpoint.io>
 * automated Tests Pipeline
 * 
 */


import io.polarpoint.workflow.ConfigurationContext
import io.polarpoint.utils.Slack

def call(ConfigurationContext context, String targetBranch, HashMap test_requirements, scmVars) {
    def stageHandlers = context.getConfigurableStageHandlers()
    def integrationTests = []
    def allTests = []
    def label = "selenium"
    def success = false
    def Slack  = new Slack(this)
    def HtmlImageGenerator = new HtmlImageGenerator(this)

    String targetEnvironment = "${context.application}-${targetBranch}"
    String uri = ${context.uri}
    String imageName= ${context.imageName}

    // run this initially on the Jenkins master
    node('master') {
        Slack.sender(true, [ buildStatus: 'STARTED' ])
        echo("target_branch:" + targetBranch)
        checkout scm

        echo("load integration tests..." + stageHandlers.integrationTests)
        for (String test : stageHandlers.integrationTests) {
            echo("load integration tests:" + test)
            integrationTests.add(load("${test}"))
        }

        allTests.addAll(integrationTests)



    }


    podTemplate(label: label) {
        node(label) {
            sh "id"
            checkout scm
            container('gradle-chrome') {
                milestone(label: 'Tests')
                try {
                    withCredentials([
                            usernamePassword(credentialsId: 'svc-nexus-user', usernameVariable: 'ORG_GRADLE_PROJECT_nexusUsername', passwordVariable: 'ORG_GRADLE_PROJECT_nexusPassword')])
                            {// use Nexus credentials for all stages

                                Slack.sender(true, [buildStatus: 'PROGRESS'])


                                milestone(label: 'Tests')
                                stage("Automated tests") {
                                    def integrationSchedule = [:]
                                    for (Object testClass : integrationTests) {
                                        def currentTest = testClass
                                        integrationSchedule[currentTest.name()] = {
                                            currentTest.runTest(targetBranch, context, test_requirements)
                                        }
                                    }
                                    try {
                                        parallel integrationSchedule
                                        milestone(label: 'automated tests')
                                    } catch (err) {
                                        err.printStackTrace()
                                        echo err.message
                                        echo("automated tests failed")
                                        throw err
                                    } finally {
                                        //
                                    }
                                }
                                success = true
                            }
                } catch (err) {
                    echo err.message
                    echo("some mandatory steps have failed, Aborting ")
                    throw err
                } finally {

                    // Notify----------------------------//
                    stage("Notify") {
                        try {
                            GitHubNotify(scmVars, 'Sonar Quality Gate', 'jenkinsci/sonar-quality', 'SUCCESS')
                            if (success) {
                                // Update confluence with build results
                                //invokeConfluence(targetBranch, context, 'SUCCESS')
                                def config = [
                                        buildStatus: 'SUCCESS',
                                        uri: '${uri}',
                                        imageName: '${imageName}'

                                ];
                                Slack.sender(config)
                                HtmlImageGenerator.imager(config)

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
                            echo "Notifications failed."
                        } finally {

                        }

                    }
                }
            }
        }
    }
}
return this;
