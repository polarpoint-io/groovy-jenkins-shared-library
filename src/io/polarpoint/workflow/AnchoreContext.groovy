/*
 * Surj Bains  <surj@polarpoint.io>
 * GenericContext
 */

package io.polarpoint.workflow


@SuppressWarnings('FieldName')
class AnchoreContext implements  Serializable {

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableAnchoreHandlers configurableAnchoreHandlers


    AnchoreContext(String application, HashMap config) {
        this.application = application
        this.config = config

    }

    AnchoreContext(String application, String configuration, String ws = null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))



          def qualityTests = config.qualityTests
        this.configurableAnchoreHandlers = new ConfigurableAnchoreHandlers(
                qualityTests
)

    }
}


