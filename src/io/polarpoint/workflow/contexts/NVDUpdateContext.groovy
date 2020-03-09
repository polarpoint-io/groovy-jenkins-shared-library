/*
 * Surj Bains  <surj@polarpoint.io>
 * NVDUpdateContext
 */


package io.polarpoint.workflow.contexts

import io.polarpoint.workflow.handlers.ConfigurableNVDUpdateHandlers


@SuppressWarnings('FieldName')
class NVDUpdateContext implements Serializable {

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableNVDUpdateHandlers configurableNVDUpdateHandlers


    NVDUpdateContext(String application, HashMap config) {
        this.application = application
        this.config = config

    }

    NVDUpdateContext(String application, String configuration, String ws = null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))



        def workspaceLibs = "${ws}@libs"

        def report = config.reporter ?: "${workspaceLibs}/pipeline-library/nvd-updater-pipeline/report/reporter.groovy"
        def apply = config.apply ?: "${workspaceLibs}/pipeline-library/nvd-updater-pipeline/apply/applier.groovy"


        this.configurableNVDUpdateHandlers = new io.polarpoint.workflow.handlers.ConfigurableNVDUpdateHandlers(
                report,
                apply)

    }
}


