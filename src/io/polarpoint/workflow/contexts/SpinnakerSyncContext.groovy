/*
 * Surj Bains  <surj@polarpoint.io>
 * SpinnakerSyncContext
 */


package io.polarpoint.workflow.contexts

import io.polarpoint.workflow.handlers.ConfigurableSpinnakerSyncHandlers


@SuppressWarnings('FieldName')
class SpinnakerSyncContext implements Serializable {

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableSpinnakerSyncHandlers configurableSpinnakerHandlers


    SpinnakerSyncContext(String application, HashMap config) {
        this.application = application
        this.config = config

    }

    SpinnakerSyncContext(String application, String configuration, String ws = null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))



        def workspaceLibs = "${ws}@libs"

        def init = config.init ?: "${workspaceLibs}/pipeline-library/spinnaker-sync-pipeline/init/initialiser.groovy"
        def apply = config.apply ?: "${workspaceLibs}/pipeline-library/spinnaker-sync-pipeline/apply/applier.groovy"


        this.configurableSpinnakerHandlers = new io.polarpoint.workflow.handlers.ConfigurableSpinnakerSyncHandlers(
                init,
                apply)

    }
}


