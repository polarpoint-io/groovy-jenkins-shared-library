/*
 * Surj Bains  <surj@polarpoint.io>
 * helm workflow
 * This Pipeline is used to run helm
 *
 */

import io.polarpoint.workflow.contexts.HelmContext
import io.polarpoint.utils.Slack

def call(HelmContext context, String targetBranch, scmVars, Boolean toTag) {
    def stageHandlers = context.getConfigurableHelmHandlers()
    def packager
    def success = false
    def label = "helm"
    def Slack  = new Slack(this)

    node('master') {

        Slack.sender(true, [ buildStatus: 'STARTED' ])

        echo("target_branch:" + targetBranch)
        checkout scm

        echo("Loading Packager: " + stageHandlers.getPackager())
        packager = load(stageHandlers.getPackager())
    }

    try {
        Slack.sender(true, [ buildStatus: 'PROGRESS' ])

        podTemplate(label: label) {
            node('helm') {
                container('helm') {

                    stage("package") {
                        packager.init(targetBranch, context, toTag)
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
