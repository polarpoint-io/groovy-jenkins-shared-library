/*
 * Surj Bains  <surj@polarpoint.io>
 * GenericContext
 */

package io.polarpoint.workflow


@SuppressWarnings('FieldName')
class NodeContext implements Serializable {

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableNodeHandlers configurableNodeHandlers

    NodeContext(String application, String configuration, String ws=null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))

        if (ws.contains('@')) {
            ws = ws.substring(0, ws.lastIndexOf("@"))
        }

        def workspaceLibs = "${ws}@libs"


        def publisher = config?.publisher ?: "${workspaceLibs}/pipeline-library/node-pipeline/publish/publisher.groovy"
        def deployer = config?.deployer ?: "${workspaceLibs}/pipeline-library/node-pipeline/deploy/deployer.groovy"
        def unitTests = config?.unitTests ?: ["${workspaceLibs}/pipeline-library/node-pipeline/tests/unit.groovy"]
        def builder = config?.builder ?: "${workspaceLibs}/pipeline-library/node-pipeline/builder.groovy"
        def containerBuilder = config?.containerBuilder ?: "${workspaceLibs}/pipeline-library/node-pipeline/containerBuild/builder.groovy"
        def containerPublisher = config?.containerPublisher ?: "${workspaceLibs}/pipeline-library/node-pipeline/containerPublish/publisher.groovy"
        def containerStager = config?.containerStager ?: "${workspaceLibs}/pipeline-library/node-pipeline/containerStage/stager.groovy"
        def containerScanner = config?.containerScanner ?: "${workspaceLibs}/pipeline-library/node-pipeline/containerScan/scanner.groovy"

        this.configurableNodeHandlers = new ConfigurableNodeHandlers(
                unitTests,
                deployer,
                builder,
                publisher,
                containerBuilder,
                containerPublisher,
                containerStager,
                containerScanner
        )

    }
}


