/*
 * Surj Bains  <surj@polarpoint.io>
 * GenericContext
 */


package io.polarpoint.workflow


@SuppressWarnings('FieldName')
class DockerImageContext implements Serializable {

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableImageHandlers configurableImageHandlers


    DockerImageContext(String application, HashMap config) {


        this.application = application
        this.config = config

    }

    DockerImageContext(String application, String configuration, String ws = null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))

        def workspaceLibs = "${ws}@libs"

        def containerrun = config.containerrun ?: "${workspaceLibs}/pipeline-library/docker-image-pipeline/container/container.groovy"
        def integrationTests = config?.integrationTests ?: ["${workspaceLibs}/pipeline-library/java-pipeline/tests/bdd.groovy"]


        this.configurableImageHandlers = new ConfigurableImageHandlers(integrationTests,
                containerrun,
                null,
                null)

    }
}


