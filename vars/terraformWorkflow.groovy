/*
 * Surj Bains  <surj@polarpoint.io>
 * terraform workflow
 * This Pipeline is used to run terraform
 *
 */

import io.polarpoint.workflow.contexts.TerraformContext

def call(TerraformContext context, String targetBranch, scmVars, Boolean toTag) {
    def stageHandlers = context.getConfigurableTerraformHandlers()
    def initialiser
    def planner
    def applier
    def success = false
    def label = "terraform"

    node('master') {


        echo("target_branch:" + targetBranch)
        checkout scm


        echo("Loading Initialiser: " + stageHandlers.getInit())
        initialiser = load(stageHandlers.getInit())

        echo("Loading Planner: " + stageHandlers.getPlan())
        planner = load(stageHandlers.getPlan())

        echo("Loading Applier: " + stageHandlers.getApply())
        applier = load(stageHandlers.getApply())



    }

    try {
        def kubernetesEnvironments = context.config.terraform.kubernetesEnvironments
        def applyTerraformWhiteList = context.config.applyTerraformWhiteList

        echo "kubernetesEnvironments : ${kubernetesEnvironments}"
        echo "applyTerraformWhiteList : ${applyTerraformWhiteList}"

        for ( def kubernetesEnvironment : kubernetesEnvironments) {

            def applyMatch = applyTerraformWhiteList.find { it == kubernetesEnvironment }
            echo "applyMatch : ${applyMatch}"
            echo("Environment : '${kubernetesEnvironment}'")


                    podTemplate(label: label) {
                    node('terraform') {
                        container('terraform') {

                            stage("init: ${kubernetesEnvironment}") {
                                initialiser.init(targetBranch, context,kubernetesEnvironment)
                            }
                            stage("plan: ${kubernetesEnvironment}") {
                                planner.plan(targetBranch, context,kubernetesEnvironment)
                            }

                            if (applyMatch !=null) {
                                    // only apply if whitelisted
                                stage("apply: ${kubernetesEnvironment}") {
                                    applier.apply(context)
                                }
                            }
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
