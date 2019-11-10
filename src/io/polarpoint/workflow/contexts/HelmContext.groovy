/*
 * Surj Bains  <surj@polarpoint.io>
 * HelmContext
 */


package io.polarpoint.workflow.contexts

import io.polarpoint.workflow.handlers.ConfigurableHelmHandlers


@SuppressWarnings('FieldName')
class HelmContext implements Serializable {

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableHelmHandlers configurableHelmHandlers


    HelmContext(String application, HashMap config) {
        this.application = application
        this.config = config

    }

    HelmContext(String application, String configuration, String ws = null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))



        def workspaceLibs = "${ws}@libs"

        def packager = config.init ?: "${workspaceLibs}/pipeline-library/helm-pipeline/package/packager.groovy"

        this.configurableHelmHandlers = new io.polarpoint.workflow.handlers.ConfigurableHelmHandlers(
                packager)
    }
}


