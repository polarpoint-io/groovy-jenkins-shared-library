import io.polarpoint.workflow.contexts.PowershellContext

def call(PowershellContext context, String targetBranch, scmVars, Boolean toTag) {
    def stageHandlers = context.getConfigurablePowershellHandlers()
    def applier
    def success = false
    def label = "powershell"

    node('master') {

        echo("target_branch:" + targetBranch)
        checkout scm

        echo("Loading Applier: " + stageHandlers.getApply())
        applier = load(stageHandlers.getApply())

    }

    try
    {
        podTemplate(label: label)
        {
            node('powershell')
            {
                container('powershell')
                {
                    stage("apply")
                    {
                        applier.apply(targetBranch, context)
                    }
                }
            }
        }
        success = true
    }

    catch (err) {

        echo err.message
        error("Powershell tasks have failed.")

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
