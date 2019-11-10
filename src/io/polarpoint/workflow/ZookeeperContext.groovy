/*
 * Surj Bains  <surj@polarpoint.io>
 * ZookeeperContext
 */

package io.polarpoint.workflow


@SuppressWarnings('FieldName')
class ZookeeperContext implements Serializable {

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableNodeHandlers configurableNodeHandlers

    ZookeeperContext(String application, String configuration, String ws=null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))

        if (ws.contains('@')) {
            ws = ws.substring(0, ws.lastIndexOf("@"))
        }

        def workspaceLibs = "${ws}@libs"


        def publisher = config?.publisher ?: "${workspaceLibs}/pipeline-library/zookeeper-pipeline/publish/publisher.groovy"
        def deployer = config?.deployer ?: "${workspaceLibs}/pipeline-library/zookeeper-pipeline/deploy/deployer.groovy"
        def unitTests = config?.unitTests ?: ["${workspaceLibs}/pipeline-library/zookeeper-pipeline/tests/unit.groovy"]
        def builder = config?.builder ?: "${workspaceLibs}/pipeline-library/zookeeper-pipeline/build/builder.groovy"
        def containerBuilder = config?.containerBuilder ?: "${workspaceLibs}/pipeline-library/zookeeper-pipeline/containerBuild/builder.groovy"
        def containerPublisher = config?.containerPublisher ?: "${workspaceLibs}/pipeline-library/zookeeper-pipeline/containerPublish/publisher.groovy"

        this.configurableNodeHandlers = new ConfigurableNodeHandlers(
                unitTests,
                deployer,
                builder,
                publisher,
                containerBuilder,
                containerPublisher
        )

    }
}


