/*
 * Surj Bains  <surj@polarpoint.io>
 * NVDUpdateContext
 */


package io.polarpoint.workflow.contexts

import io.polarpoint.workflow.handlers.ConfigurableGrafanaHandlers


@SuppressWarnings('FieldName')
class GrafanaContext implements Serializable {

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableGrafanaHandlers configurableGrafanaHandlers


    GrafanaContext(String application, HashMap config) {
        this.application = application
        this.config = config

    }

    GrafanaContext(String application, String configuration, String ws = null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))



        def workspaceLibs = "${ws}@libs"

        def report = config.reporter ?: "${workspaceLibs}/pipeline-library/grafana-pipeline/report/reporter.groovy"
        def apply = config.apply ?: "${workspaceLibs}/pipeline-library/grafana-pipeline/apply/applier.groovy"


        this.configurableGrafanaHandlers = new io.polarpoint.workflow.handlers.ConfigurableGrafanaHandlers(
                report,
                apply)

    }
}


