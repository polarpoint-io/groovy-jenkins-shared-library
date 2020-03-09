/*
 * Surj Bains  <surj@polarpoint.io>
 * execute git rob pipeline to scan repos for credentials
 */


import io.polarpoint.workflow.contexts.NVDUpdateContext

def call(NVDUpdateContext context, String targetBranch) {
    def stageHandlers = context.getConfigurableNVDUpdateHandlers()
    def reporter
    def applier
    def success = false
    def label = "gradle-6-0-1"

    node('master') {


        echo("target_branch:" + targetBranch)
        checkout scm

        echo("Loading Applier: " + stageHandlers.getApply())
        applier = load(stageHandlers.getApply())

        echo("Loading Reporter: " + stageHandlers.getReport())
        reporter = load(stageHandlers.getReport())


    }

    try {



        podTemplate(label: label) {
            node('gradle-6-0-1') {
                container('gradle-6-0-1') {

                    stage("apply") {
                        applier.apply(targetBranch, context)
                    }
                    stage("reporter") {

                        reporter.report(targetBranch, context)
                    }
                }
            }
        }



        success = true
    }

    catch (err) {

        echo err.message
        error("OWASP NVD Updater tasks have failed.")

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
