/*
 * Surj Bains  <surj@polarpoint.io>
 * execute git rob pipeline to scan repos for credentials
 */


import io.polarpoint.workflow.contexts.GrafanaContext

def call(GrafanaContext context, String targetBranch) {
    def stageHandlers = context.getConfigurableGrafanaHandlers()
    def reporter
    def applier
    def success = false
    def label = "curljq"

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
            node('curljq') {
                container('curljq') {

                    stage("apply") {
                        applier.apply(targetBranch, context)
                    }
                }
                container('jnlp') {

                    stage("apply") {
                        reporter.report(targetBranch, context)
                    }
                }

            }
        }

        success = true
    }

    catch (err) {

        echo err.message
        error("OWASP  Grafana exporter  tasks have failed.")

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
