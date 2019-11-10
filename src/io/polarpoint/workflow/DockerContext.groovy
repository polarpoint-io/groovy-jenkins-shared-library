/*
 * Surj Bains  <surj@polarpoint.io>
 * GenericContext
 */


package io.polarpoint.workflow


@SuppressWarnings('FieldName')
class DockerContext implements Serializable {

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableDockerHandlers dockerHandlers


    DockerContext(String application, HashMap config) {
        this.application = application
        this.config = config

    }

    DockerContext(String application, String configuration, String ws = null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))



        def workspaceLibs = "${ws}@libs"

        def builder = config.builder ?: "${workspaceLibs}/pipeline-library/docker-pipeline/build/builder.groovy"
        def stager = config.staging ?: "${workspaceLibs}/pipeline-library/docker-pipeline/stage/stager.groovy"
        def publisher = config?.publisher ?: "${workspaceLibs}/pipeline-library/docker-pipeline/publish/publisher.groovy"
        def scanner = config?.scanner ?: "${workspaceLibs}/pipeline-library/docker-pipeline/scan/scanner.groovy"
        def qualityTests = config?.qualityTests ?: ["${workspaceLibs}/pipeline-library/docker-pipeline/tests/goss.groovy"]


        this.dockerHandlers = new ConfigurableDockerHandlers(
                qualityTests,
                scanner,
                builder,
                stager,
                publisher)

    }
}


