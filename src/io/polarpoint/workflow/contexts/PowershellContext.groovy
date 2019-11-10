package io.polarpoint.workflow.contexts

import io.polarpoint.workflow.handlers.ConfigurablePowershellHandlers



@SuppressWarnings('FieldName')
class PowershellContext implements Serializable {

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurablePowershellHandlers configurablePowershellHandlers


    PowershellContext(String application, HashMap config) {
        this.application = application
        this.config = config

    }

    PowershellContext(String application, String configuration, String ws = null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))



        def workspaceLibs = "${ws}@libs"

        def report = config.reporter ?: "${workspaceLibs}/pipeline-library/powershell-pipeline/report/reporter.groovy"
        def apply = config.apply ?: "${workspaceLibs}/pipeline-library/powershell-pipeline/apply/applier.groovy"


        this.configurablePowershellHandlers = new io.polarpoint.workflow.handlers.ConfigurablePowershellHandlers(
                report,
                apply)

    }
}


