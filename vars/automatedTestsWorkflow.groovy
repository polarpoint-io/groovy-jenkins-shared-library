/*
 * Surj Bains  <surj@polarpoint.io>
 * automated Tests Pipeline
 * 
 */


import io.polarpoint.workflow.ConfigurationContext
import io.polarpoint.utils.Slack

import javax.swing.JFileChooser

def call(ConfigurationContext context, String targetBranch, HashMap test_requirements, scmVars) {
    def stageHandlers = context.getConfigurableStageHandlers()
    def integrationTests = []
    def allTests = []
    def label = "selenium"
    def success = false
    def Slack  = new Slack(this)

    String targetEnvironment = "${context.application}-${targetBranch}"

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

                            podTemplate(label: 'phantomjs') {
                                node('phantomjs') {
                                    container('phantomjs') {
                                        stage("Publish") {
                                            unstash 'archive'
                                            sh "/usr/local/bin/phantomjs /rasterize.js target/site/serenity/serenity-summary.html serenity-summary.png  800px*385px"
                                            archiveArtifacts artifacts: "serenity-summary.png", onlyIfSuccessful: true
                                            stash includes: 'serenity-summary.png', name: 'serenity-summary'
                                            sh "ls -rtl"
                                        }
                                    }
                                }
                            }

                            if (success) {
                                def config = [
                                        buildStatus: 'SUCCESS',
                                        filename: 'serenity-summary.png'
                                ];

                                Slack.sender(config)
                                unstash  name: 'serenity-summary'
                                def serenitySummary = readFile encoding: 'Base64', file: 'serenity-summary.png'
                                //slackUploadFile channel: "#dev-ops-common-builds", filePath: "serenity-summary.png", initialComment: buildStatus
                                Slack.fileSender( config,serenitySummary)

                            } else {
                                def config = [
                                        buildStatus: 'FAILURE',
                                        filename: 'serenity-summary.png'
                                ];
                                Slack.sender(config)
                                unstash  name: 'serenity-summary'
                                def serenitySummary = readFile encoding: 'Base64', file: 'serenity-summary.png'
                                //slackUploadFile channel: "#dev-ops-common-builds", filePath: "serenity-summary.png", initialComment: buildStatus
                                Slack.fileSender( config,serenitySummary)
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