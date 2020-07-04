/*
 * Surj Bains  <surj@polarpoint.io>
 * Java workflow
 * This Pipeline is used to version and build all branches
 *
 */

import io.polarpoint.workflow.contexts.Javav_0_2_0ConfigurationContext
import io.polarpoint.utils.Slack

def call(Javav_0_2_0ConfigurationContext context, String targetBranch, scmVars, Boolean toTag) {
    def stageHandlers = context.getConfigurableStageHandlers()
    def builder
    def deployer
    def publisher
    def containerPublisher
    def containerBuilder
    def containerScanner
    def containerStager
    def chgLogger
    def unitTests = []
    def qualityTests = []
    def staticAnalysisTests = []
    def integrationTests = []
    def allTests = []
    def success = false
    def label = "gradle-6"
    def gateStatus
    def dockerFileExists
    def Slack  = new Slack(this)
    String targetEnvironment = "${context.application}-${targetBranch}"
    def vulnerabilityImageScanner


    node('master') {


        Slack.sender(true, [ buildStatus: 'STARTED' ])

        echo("target_branch:" + targetBranch)
        checkout scm

        dockerFileExists = fileExists("${env.WORKSPACE}/Dockerfile")

        echo("Loading builder: " + stageHandlers.builder)
        builder = load(stageHandlers.builder)

        echo("Loading publisher: " + stageHandlers.publisher)
        publisher = load(stageHandlers.publisher)

        echo("load unit tests..." + stageHandlers.unitTests)
        for (String test : stageHandlers.unitTests) {
            echo("load unit tests:" + test)
            unitTests.add(load("${test}"))
        }
        echo("load quality tests..." + stageHandlers.qualityTests)
        for (String test : stageHandlers.qualityTests) {
            echo("load quality tests:" + test)
            qualityTests.add(load("${test}"))
        }
        echo("load static analysis tests..." + stageHandlers.staticAnalysisTests)
        for (String test : stageHandlers.staticAnalysisTests) {
            echo("load sanity tests:" + test)
            staticAnalysisTests.add(load("${test}"))
        }
        echo("load integration tests..." + stageHandlers.integrationTests)
        for (String test : stageHandlers.integrationTests) {
            echo("load integration tests:" + test)
            integrationTests.add(load("${test}"))
        }

        echo("Loading container builder: " + stageHandlers.containerBuilder)
        containerBuilder = load(stageHandlers.containerBuilder)

        echo("Loading container stager: " + stageHandlers.containerStager)
        containerStager = load(stageHandlers.containerStager)

        echo("Loading container scanner: " + stageHandlers.containerScanner)
        containerScanner = load(stageHandlers.containerScanner)

        echo("Loading container publisher: " + stageHandlers.containerPublisher)
        containerPublisher = load(stageHandlers.containerPublisher)

        echo("Loading container Vulnerability Dagda: " + stageHandlers.vulnerabilityScanner)
        vulnerabilityImageScanner = load(stageHandlers.vulnerabilityScanner)

        allTests.addAll(unitTests)
        allTests.addAll(qualityTests)
        allTests.addAll(staticAnalysisTests)

        echo("Loading deployer: " + stageHandlers.deployer)
        deployer = load(stageHandlers.deployer)

        echo("Loading chgLogger: " + stageHandlers.chgLogger)
        chgLogger = load(stageHandlers.chgLogger)

    }

    try {

        Slack.sender(true, [ buildStatus: 'PROGRESS' ])

        withCredentials([
                usernamePassword(credentialsId: 'service-nexus-user', usernameVariable: 'ORG_GRADLE_PROJECT_nexusUsername', passwordVariable: 'ORG_GRADLE_PROJECT_nexusPassword')])
                {
                    // use Nexus credentials for all stages

            milestone(label: 'Static Analysis')
            stage("Quality Tests") {
                def codeSanitySchedule = [:]
                for (Object testClass : staticAnalysisTests) {
                    def currentTest = testClass
                    codeSanitySchedule[currentTest.name()] = {
                        currentTest.runTest(targetBranch, context)
                    }
                }

                    for (Object qualityTestClass : qualityTests) {
                        def currentqualityTest = qualityTestClass
                        codeSanitySchedule[currentqualityTest.name()] = {
                            currentqualityTest.runTest(targetBranch, context)
                        }
                    }


                parallel codeSanitySchedule
            }


            stage("Quality Gate") {
                timeout(time: 1, unit: 'HOURS') {
                    def qg = waitForQualityGate()
                    if (qg.status == 'OK') {
                        gateStatus = 'SUCCESS'
                    } else {
                        gateStatus = 'FAILURE'
                    }
                }
            }

                    Slack.sender(true, [buildStatus: 'PASSED-QUALITY-TESTS'])
                    milestone(label: 'Integration')
                    stage("Integration Tests") {
                        def codeSanitySchedule = [:]
                        for (Object testClass : integrationTests) {
                            def currentTest = testClass
                            codeSanitySchedule[currentTest.name()] = {
                                currentTest.runTest(targetBranch, context)
                            }
                        }

                        parallel codeSanitySchedule
                    }

                    Slack.sender(true, [buildStatus: 'PASSED-INTEGRATION-TESTS'])

            milestone(label: 'Build')
            podTemplate(label: 'gradle-6') {
                node('gradle-6') {
                    cleanWs()
                    container('gradle-6') {
                        checkout scm

                        stage("Build") {
                            builder.build(targetBranch, context)
                        }
                        
                        if (toTag) {

                            // Create the change log
                            stage("Change log") {
                                chgLogger.createlog(targetBranch, context)
                            }

                            milestone(label: 'Tag')
                            stage("Tag") {
                                invokeSemanticVersioningV2(targetBranch, context)
                            }
                        }
                    }
                }
            }                      

            milestone(label: 'Publish')
            podTemplate(label: 'maven') {
                node('maven') {
                    container('maven') {
                        stage("Publish") {
                            publisher.publish(targetBranch, context)
                        }
                    }
                }
            }

            if (dockerFileExists) {

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
                        stage("Docker Container/Vulnerability Scanner") {

                            parallel(
                                    anchore: {
                                        container('docker') {
                                            containerScanner.scan(targetBranch, context)
                                        }
                                    },
                                    dagda: {
                                        node('curljq') {

                                                container('curljq') {
                                                    vulnerabilityImageScanner.scan(targetBranch, context)
                                                }

                                            }
                                    }
                            )



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
                GitHubNotify(scmVars, 'Sonar Quality Gate', 'jenkinsci/sonar-quality', gateStatus)


                if (success) {
                    // Update confluence with build results
                    //invokeConfluence(targetBranch, context, 'SUCCESS')
                    def config = [
                            buildStatus: 'SUCCESS',
                            image: 'true'
                    ];
                    Slack.sender(config)

                   // slackSend(color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")


                } else {
                    // Update confluence with build results
                    //invokeConfluence(targetBranch, context, 'FAILURE')
                    def config = [
                            buildStatus: 'FAILURE',
                            image: 'true'
                    ];
                    Slack.sender(config)
                    //slackSend(color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")

                    echo "Pipeline tasks have failed."
                }

            } catch (err) {
                println err.message
                //echo Utils.stackTrace(error)
                error "Notifications failed."
            } finally {

                // do nothing
            }
        }
    }
}

return this;
