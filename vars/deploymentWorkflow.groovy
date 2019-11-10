/*
 * Surj Bains  <surj@polarpoint.io>
 * integration Pipeline
 * This Pipeline is used to promote artifacts to
 * higher environments and initiate integration tests
 */


import io.polarpoint.workflow.ConfigurationContext

def call(ConfigurationContext context, String targetBranch, String deploymentNotesMD) {
    def stageHandlers = context.getConfigurableStageHandlers()
    def deployer
    def cf_space = context.config.cloudFoundry.cloudSpace


    String targetEnvironment = "${context.application}-${targetBranch}"

    def success = false

    // run this initially on the Jenkins master
    node('master') {
        echo("target_branch:" + targetBranch)
        checkout scm

        echo("Loading deployer: " + stageHandlers.deployer)
        deployer = load(stageHandlers.deployer)


    }

    milestone(label: 'Deploy')
    try {
        // Locked Deploy Cycle-----------------//
        lock(inversePrecedence: true, quantity: 1, resource: targetEnvironment) {
            stage("Deploy") {
                deployer.deploy(targetBranch, context)
            }

        }
        milestone(label: 'Tag')
        stage("Tag") {

            invokeSemanticVersioning(targetBranch, context)
        }
        success = true

    } catch (error) {
        echo error.message
        echo("Deployment has failed, Aborting ")
        throw error
    } finally {

        // Notify----------------------------//
        stage("Notify") {
            try {
                if (success) {

                    // Update confluence with build results
                    invokeConfluence(targetBranch, context, 'SUCCESS')
                    slackSend(color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
                    slackSend(color: '#00FF00', channel: '#jenkins-deployments',  message: "SUCCESSFUL: ``` ${deploymentNotesMD} ```")
                    emailext (
                                    subject: "Deployment to : '${cf_space} [${env.BUILD_NUMBER}]'",
                                    body: """<p>Completed Successfully : Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
                                    ${deploymentNotesMD}
                                    <p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>""",
                                    recipientProviders: [[$class: 'UpstreamComitterRecipientProvider']]
                            )

                } else {
                    // Update confluence with build results
                    invokeConfluence(targetBranch, context, 'FAILURE')
                    slackSend(color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
                    slackSend(color: '#FF0000', channel: '#jenkins-deployments',  message: "FAILED:  ``` ${deploymentNotesMD}  (${env.BUILD_URL})```")
                    emailext (
                            subject: "Deployment to : '${cf_space} [${env.BUILD_NUMBER}]'",
                            body: """<p>Failed : Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
                                    ${deploymentNotesMD}
                                    <p>Check console output at "<a href="${env.BUILD_URL}">${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>"</p>""",
                            recipientProviders: [[$class: 'UpstreamComitterRecipientProvider']]
                    )
                    echo "Pipeline tasks have failed."
                }

            } catch (error) {
                echo error.message
                //echo Utils.stackTrace(error)
                echo "Notifications failed."
            } finally {

            }

        }
    }
}



return this;
