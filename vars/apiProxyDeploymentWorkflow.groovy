/*
 * Surj Bains  <surj@polarpoint.io>
 * integration Pipeline
 * This Pipeline is used to promote artifacts to
 * higher environments and initiate integration tests
 */


import io.polarpoint.workflow.ConfigurationContext


def call(ConfigurationContext context, String targetBranch) {
    def stageHandlers = context.getConfigurableStageHandlers()
    def deployer

    def success = false


    String targetEnvironment = "${context.application}-${targetBranch}"

    // run this initially on the Jenkins master
    node('master') {
        echo("target_branch:" + targetBranch)
        checkout scm

        echo("Loading deployer: " + stageHandlers.deployer)
        deployer = load(stageHandlers.deployer)


    }

    milestone(label: 'Deploy')
    try {

        lock(inversePrecedence: true, quantity: 1, resource: targetEnvironment) {
            stage("Deploy") {
                deployer.deploy(targetBranch, context)
            }

        }

        success = true

    } catch (err) {
        echo err.message
        error("some mandatory steps have failed, Aborting ")

    } finally {

        // Notify----------------------------//
        stage("Notify") {
            try {
                if (success) {

                    // Update confluence with build results
                    invokeConfluence(targetBranch, context, 'SUCCESS')
                    slackSend(color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
                    def comment = "${BUILD_URL} FAILED"
                    jiraComment body: comment, issueKey: env.BRANCH_NAME


                } else {
                    // Update confluence with build results
                    invokeConfluence(targetBranch, context, 'FAILURE')
                    slackSend(color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
                    def comment = "${BUILD_URL} FAILED"
                    jiraComment body: comment, issueKey: env.BRANCH_NAME


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
