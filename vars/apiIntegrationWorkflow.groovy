/*
 * Surj Bains  <surj@polarpoint.io>
 * integration Pipeline
 * This Pipeline is used to promote artifacts to
 * higher environments and initiate integration tests
 */


import io.polarpoint.utils.Slack
import io.polarpoint.workflow.ApiIntegrationContext

def call(ApiIntegrationContext context, String targetBranch, HashMap test_requirements) {
    def stageHandlers = context.configurableApiHandlers
    def integrationTests = []
    def allTests = []
    def success = false
    def Slack  = new Slack(this)

    def nodePodYaml = '''
apiVersion: v1
kind: Pod
metadata:
  generateName: agent-k8s-
  labels:
    name: gradle49
spec:
  containers:
  - name: jnlp
    image: jenkins/jnlp-slave:3.26-1
    tty: true
    volumeMounts:
    - mountPath: /etc/ssl/certs/ca-certificates.crt
      name: volume-ca-bundle
      subPath: ca-certificates.crt
    - mountPath: /etc/ssl/certs/java/cacerts
      name: volume-ca-bundle
      subPath: cacerts
    - mountPath: /etc/default/cacerts
      name: volume-ca-bundle
      subPath: cacerts
    securityContext:
      allowPrivilegeEscalation: false
  - command:
    - "cat"
    image: "gradle:4.9-jdk8"
    imagePullPolicy: "IfNotPresent"
    name: "gradle49"
    tty: true
    volumeMounts:
    - mountPath: /etc/ssl/certs/ca-certificates.crt
      name: volume-ca-bundle
      subPath: ca-certificates.crt
    - mountPath: /etc/ssl/certs/java/cacerts
      name: volume-ca-bundle
      subPath: cacerts
    - mountPath: /etc/default/cacerts
      name: volume-ca-bundle
      subPath: cacerts
  dnsConfig:
    options:
    - name: ndots
      value: "1"
  resources:
      limits:
        cpu: "2"
        memory: '2Gi'
  securityContext:
    runAsUser: 10000
    fsUser: 2000
    allowPrivilegeEscalation: false
  imagePullPolicy: IfNotPresent 
  imagePullSecrets:
    - name: registry-credentials 
  volumes:
  - configMap:
      defaultMode: 420
      name: ca-bundle
    name: volume-ca-bundle
'''
withCredentials([
            usernamePassword(credentialsId: 'svc-nexus-user', usernameVariable: 'ORG_GRADLE_PROJECT_nexusUsername', passwordVariable: 'ORG_GRADLE_PROJECT_nexusPassword')])
        {

            node('master') {

                unstash('pipelines')

                // run this initially on the Jenkins master
                echo("target_branch:" + targetBranch)
                echo("load integration tests..." + stageHandlers.integrationTests)
                for (String test : stageHandlers.integrationTests) {
                    echo("load integration tests:" + test)
                    integrationTests.add(load("${test}"))
                }

                allTests.addAll(integrationTests)
            }

            Slack.sender(true, [ buildStatus: 'STARTED' ])

            def label = "gradle-${UUID.randomUUID().toString()}"
            podTemplate(
                    label: label,
                    yaml: nodePodYaml,
                    nodeUsageMode: 'EXCLUSIVE',
                    podRetention: onFailure(),
                    idleMinutes: 1)
                    {
                        node(label) {
                            container('gradle49') {

                                def integrationSchedule = [:]
                                for (Object testClass : integrationTests) {
                                    def currentTest = testClass
                                    integrationSchedule[currentTest.name()] = {
                                        currentTest.runTest(targetBranch, context, test_requirements)
                                    }
                                }
                                println integrationSchedule.toString()

                                parallel integrationSchedule
                                milestone(label: 'Integration tests')


                            }
                            //cleanWs()
                        }
                    }
            // Notify----------------------------//

            stage("Notify") {
                try {


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


return this;
