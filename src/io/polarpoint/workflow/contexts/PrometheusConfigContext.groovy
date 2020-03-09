/*
 * Surj Bains  <surj@polarpoint.io>
 * PrometheusConfigContext
 */


package io.polarpoint.workflow.contexts

import io.polarpoint.workflow.handlers.ConfigurablePrometheusConfigHandlers



@SuppressWarnings('FieldName')
class PrometheusConfigContext implements Serializable {

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurablePrometheusConfigHandlers configurablePrometheusConfigHandlers


    PrometheusConfigContext(String application, HashMap config) {
        this.application = application
        this.config = config

    }

    PrometheusConfigContext(String application, String configuration, String ws = null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))



        def workspaceLibs = "${ws}@libs"

        def validate = config.validate ?: "${workspaceLibs}/pipeline-library/prometheus-alerts-config-pipeline/validate/validator.groovy"
        def apply = config.apply ?: "${workspaceLibs}/pipeline-library/prometheus-alerts-config-pipeline/apply/applier.groovy"


        this.configurablePrometheusConfigHandlers = new io.polarpoint.workflow.handlers.ConfigurablePrometheusConfigHandlers(
                validate,
                apply)

    }
}


