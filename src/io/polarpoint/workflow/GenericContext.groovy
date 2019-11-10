/*
 * Surj Bains  <surj@polarpoint.io>
 * GenericContext
 */

package io.polarpoint.workflow




@SuppressWarnings('FieldName')
class GenericContext implements  Serializable {

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableGenericHandlers configurableGenericHandlers


    GenericContext(String application, HashMap config) {
        this.application = application
        this.config = config

    }

    GenericContext(String application, String configuration, String ws = null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))



        if (ws.contains('@')) {
            ws = ws.substring(0, ws.lastIndexOf("@"))
        }

        def workspaceLibs = "${ws}@libs"

        def builder = config.builder ?: "${workspaceLibs}/pipeline-library/apigee-pipeline/build/builder.groovy"
        def publisher = config?.publisher ?: "${workspaceLibs}/pipeline-library/apigee-pipeline/publish/publisher.groovy"
        def deployer = config?.deployer ?: "${workspaceLibs}/pipeline-library/apigee-pipeline/deploy/deployer.groovy"
        def qualityTests = config?.qualityTests ?: ["${workspaceLibs}/pipeline-library/apigee-pipeline/tests/checkstyle.groovy"]


        this.configurableGenericHandlers = new ConfigurableGenericHandlers(
                qualityTests,
                deployer,
                builder,
                publisher)

    }
}


