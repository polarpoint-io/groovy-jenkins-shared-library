/*
 * Surj Bains  <surj@polarpoint.io>
 * execute git rob pipeline to scan repos for credentials
 */


import io.polarpoint.workflow.contexts.GitRobContext

def call(GitRobContext context, String targetBranch) {
    def stageHandlers = context.getConfigurableGitRobHandlers()
    def reporter
    def applier
    def success = false
    def label = "prom-tool"

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
            node('git-rob') {
                container('git-rob') {

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
        error("Prometheus Alert Mgr  tasks have failed.")

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
