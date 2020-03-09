/*
 * Surj Bains  <surj@polarpoint.io>
 * ConfigurationContext
 */

package io.polarpoint.workflow


@SuppressWarnings('FieldName')
class ConfigurationContext implements Serializable {

    ConfigurationContext() {
    }

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableStageHandlers configurableStageHandlers

    ConfigurationContext(String application, HashMap config) {
        this.application = application
        this.config = config

    }

    ConfigurationContext(String application, String configuration, String ws = null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))


        if(ws.contains('@'))
        {
           ws = ws.substring(0, ws.lastIndexOf("@"))

        }


        def workspaceLibs = "${ws}@libs"

        def semanticVersioner = config.semanticVersioner ?: "${workspaceLibs}/pipeline-library/java-pipeline/semanticVersioning/semanticVersioner.groovy"
        def builder = config.builder ?: "${workspaceLibs}/pipeline-library/java-pipeline/build/builder.groovy"
        def publisher = config?.publisher ?: "${workspaceLibs}/pipeline-library/java-pipeline/publish/publisher.groovy"
        def deployer = config?.deployer ?: "${workspaceLibs}/pipeline-library/java-pipeline/deploy/deployer.groovy"
        def containerBuilder = config?.containerBuilder ?: "${workspaceLibs}/pipeline-library/java-pipeline/containerBuild/builder.groovy"
        def containerPublisher = config?.containerPublisher ?: "${workspaceLibs}/pipeline-library/java-pipeline/containerPublish/publisher.groovy"
        def containerStager = config?.containerStager ?: "${workspaceLibs}/pipeline-library/java-pipeline/containerStage/stager.groovy"
        def containerScanner = config?.containerScanner ?: "${workspaceLibs}/pipeline-library/java-pipeline/containerScan/scanner.groovy"
        def qualityTests = config?.qualityTests ?: ["${workspaceLibs}/pipeline-library/java-pipeline/tests/sonar.groovy"]
        def unitTests = config?.unitTests ?: ["${workspaceLibs}/pipeline-library/java-pipeline/tests/unit.groovy"]
        def staticAnalysisTests = config?.staticAnalysisTests ?: [
                "${workspaceLibs}/pipeline-library/java-pipeline/tests/checkstyle.groovy",
                "${workspaceLibs}/pipeline-library/java-pipeline/tests/findbugs.groovy"
                //"${workspaceLibs}/pipeline-library/java-pipeline/tests/owasp.groovy" Disable owasp Stage for the moment
        ]
        def integrationTests = config?.integrationTests ?: [
                "${workspaceLibs}/pipeline-library/java-pipeline/tests/bdd.groovy",
                 "${workspaceLibs}/pipeline-library/java-pipeline/tests/integration.groovy"]
        def performanceTests = config?.performanceTests ?: ["${workspaceLibs}/pipeline-library/java-pipeline/tests/performance.groovy"]
        def vulnerabilityScanner = config?.vulnerabilityScanner ?: "${workspaceLibs}/pipeline-library/java-pipeline/imageVulnScan/vulnscanner.groovy"


        configurableStageHandlers = new ConfigurableStageHandlers(
                semanticVersioner,
                builder,
                publisher,
                containerBuilder,
                containerStager,
                containerScanner,
                containerPublisher,
                deployer,
                qualityTests,
                unitTests,
                staticAnalysisTests,
                integrationTests,
                performanceTests,
                vulnerabilityScanner

                )

    }



    String asJsonString() {

        return (new groovy.json.JsonBuilder(this.config)).toPrettyString()
    }
}
