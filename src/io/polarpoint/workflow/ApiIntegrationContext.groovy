/*
 * Surj Bains  <surj@polarpoint.io>
 * GenericContext
 */

package io.polarpoint.workflow


@SuppressWarnings('FieldName')
class ApiIntegrationContext implements Serializable {

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableApiHandlers configurableApiHandlers
    final String ws


    ApiIntegrationContext(String application, String configuration, ws = null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))
        this.ws = ws

        if (ws.contains('@')) {
            ws = ws.substring(0, ws.lastIndexOf("@"))
        }

        def workspaceLibs = "${ws}@libs"
        def integrationTests = config?.qualityTests ?: ["${workspaceLibs}/pipeline-library/api-integration-pipeline/tests/bdd.groovy"]
        this.configurableApiHandlers = new ConfigurableApiHandlers(integrationTests)

    }
}


