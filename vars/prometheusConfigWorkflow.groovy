/*
 * Vishal Parmar
 * prometheus alertmanager config reporter  sync workflow
 * This Pipeline is used to validate alert mgr ocnfig for prometheus
 *
 */

import io.polarpoint.workflow.contexts.PrometheusConfigContext

def call(PrometheusConfigContext context, String targetBranch, scmVars, Boolean toTag) {
    def stageHandlers = context.getConfigurablePrometheusConfigHandlers()
    def validator
    def applier
    def success = false
    def label = "prom-tool"

    node('master') {


        echo("target_branch:" + targetBranch)
        checkout scm

        echo("Loading Applier: " + stageHandlers.getApply())
        applier = load(stageHandlers.getApply())

        echo("Loading Validator: " + stageHandlers.getValidate())
        validator = load(stageHandlers.getValidate())


    }

    try {



        podTemplate(label: label) {
            node('prom-tool') {
                container('prom-tool') {

                    stage("apply") {
                        applier.apply(targetBranch, context)
                    }
                    stage("validate") {

                        validator.validate(targetBranch, context)
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
