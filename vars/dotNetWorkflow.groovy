/*
 * Surj Bains  <surj@polarpoint.io>
 * dotNetWorkflow
 * This Pipeline is used to version and build  branches
 *
 */

import io.polarpoint.workflow.contexts.DotNetContext
import io.polarpoint.utils.Slack

def call(DotNetContext context, String targetBranch, scmVars, Boolean toTag) {
    def stageHandlers = context.getConfigurableDotNetStageHandlers()
    def builder
    def deployer
    def publisher
    def containerPublisher
    def containerBuilder
    def containerScanner
    def containerStager
    def unitTests = []
    def qualityTests = []
    def staticAnalysisTests = []
    def allTests = []
    def success = false
    def label = "jnlp"
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

    }

    try {

        Slack.sender(true, [ buildStatus: 'PROGRESS' ])

        withCredentials([
                usernamePassword(credentialsId: 'svc-nexus-user', usernameVariable: 'ORG_GRADLE_PROJECT_nexusUsername', passwordVariable: 'ORG_GRADLE_PROJECT_nexusPassword')])
                {
                    // use Nexus credentials for all stages

            // everything script loaded in this block needs a node() and container()
            // look in the dotnet-pipeline .groovy files!!!

//            milestone(label: 'Static Analysis')
//            stage("Static Analysis") {
//                def codeSanitySchedule = [:]
//                for (Object testClass : staticAnalysisTests) {
//                    def currentTest = testClass
//                    codeSanitySchedule[currentTest.name()] = {
//                        currentTest.runTest(targetBranch, context)
//                    }
//                }
//
//                parallel codeSanitySchedule
//            }


            Slack.sender(true, [buildStatus: 'PROGRESS'])

//            milestone(label: 'Quality Tests')
//            stage("Quality Tests") {
//                for (Object testClass : qualityTests) {
//                    def currentTest = testClass
//
//                    currentTest.runTest(targetBranch, context)
//                }
//            }
//
//            stage("Quality Gate") {
//                timeout(time: 1, unit: 'HOURS') {
//                    def qg = waitForQualityGate()
//                    if (qg.status == 'OK') {
//                        gateStatus = 'SUCCESS'
//                    } else {
//                        gateStatus = 'FAILURE'
//                    }
//                }
//            }

            milestone(label: 'Build')


            podTemplate(label: 'dotnet') {
                node('dotnet') {
                    container('dotnet') {

                        stage("Build") {
                            builder.build(targetBranch, context)
                        }

                        if (toTag) {
                            milestone(label: 'Tag')
                            stage("Tag") {
                                invokeSemanticVersioning(targetBranch, context)
                            }
                        }
                        milestone(label: 'Publish')
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
                                    })



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
