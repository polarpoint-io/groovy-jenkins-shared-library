/*
 * Surj Bains  <surj@polarpoint.io>
 * TerraformContext
 */


package io.polarpoint.workflow.contexts

import io.polarpoint.workflow.handlers.ConfigurableTerraformHandlers


@SuppressWarnings('FieldName')
class TerraformContext implements Serializable {

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableTerraformHandlers configurableTerraformHandlers


    TerraformContext(String application, HashMap config) {
        this.application = application
        this.config = config

    }

    TerraformContext(String application, String configuration, String ws = null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))



        def workspaceLibs = "${ws}@libs"

        def init = config.init ?: "${workspaceLibs}/pipeline-library/terraform-pipeline/init/initialiser.groovy"
        def plan = config.plan ?: "${workspaceLibs}/pipeline-library/terraform-pipeline/plan/planner.groovy"
        def visual = config.visual ?: "${workspaceLibs}/pipeline-library/terraform-pipeline/visual/visualise.groovy"
        def apply = config.apply ?: "${workspaceLibs}/pipeline-library/terraform-pipeline/apply/applier.groovy"


        this.configurableTerraformHandlers = new io.polarpoint.workflow.handlers.ConfigurableTerraformHandlers(
                init,
                plan,
                visual,
                apply)

    }
}


