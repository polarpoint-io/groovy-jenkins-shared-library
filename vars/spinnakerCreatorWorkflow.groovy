/*
 * Surj Bains  <surj@polarpoint.io>
 * spinnaker sync workflow
 * This Pipeline is used to create pipelines in spinnaker
 *
 */

import io.polarpoint.workflow.contexts.SpinnakerCreatorContext

def call(SpinnakerCreatorContext context, String targetBranch, scmVars, Boolean toTag) {
    def stageHandlers = context.getConfigurableSpinnakerHandlers()
    def initialiser
    def applier
    def success = false
    def label = "terraform"

    node('master') {


        echo("target_branch:" + targetBranch)
        checkout scm


        echo("Loading Initialiser: " + stageHandlers.getInit())
        initialiser = load(stageHandlers.getInit())

        echo("Loading Applier: " + stageHandlers.getApply())
        applier = load(stageHandlers.getApply())



    }

    try {



        podTemplate(label: label) {
            node('kubectl') {
                container('kubectl') {

                    stage("init") {
                        initialiser.init(targetBranch, context)
                    }
                    stage("apply") {
                        applier.apply(targetBranch, context)
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
                    slackSend(color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
                } else {
                    slackSend(color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")

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

return this;
