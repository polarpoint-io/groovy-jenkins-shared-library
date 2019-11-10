/*
 * Surj Bains  <surj@polarpoint.io>
 * ServerLessContext
 */

package io.polarpoint.workflow.contexts
import io.polarpoint.workflow.handlers.ConfigurableServerlessStageHandlers


@SuppressWarnings('FieldName')
class ServerlessContext implements Serializable {

    ServerlessContext() {
    }

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableServerlessStageHandlers configurableServerlessStageHandlers

    ServerlessContext(String application, HashMap config) {
        this.application = application
        this.config = config

    }

    ServerlessContext(String application, String configuration, String ws = null) {
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

        def builder = config.builder ?: "${workspaceLibs}/pipeline-library/java-serverless-pipeline/build/builder.groovy"
        def publisher = config?.publisher ?: "${workspaceLibs}/pipeline-library/java-serverless-pipeline/publish/publisher.groovy"
        def deployer = config?.deployer ?: "${workspaceLibs}/pipeline-library/java-serverless-pipeline/deploy/deployer.groovy"
        def containerBuilder = config?.containerBuilder ?: "${workspaceLibs}/pipeline-library/java-serverless-pipeline/containerBuild/builder.groovy"
        def containerPublisher = config?.containerPublisher ?: "${workspaceLibs}/pipeline-library/java-serverless-pipeline/containerPublish/publisher.groovy"
        def containerStager = config?.containerStager ?: "${workspaceLibs}/pipeline-library/java-serverless-pipeline/containerStage/stager.groovy"
        def containerScanner = config?.containerScanner ?: "${workspaceLibs}/pipeline-library/java-serverless-pipeline/containerScan/scanner.groovy"
        def qualityTests = config?.qualityTests ?: ["${workspaceLibs}/pipeline-library/java-serverless-pipeline/tests/sonar.groovy"]
        def unitTests = config?.unitTests ?: ["${workspaceLibs}/pipeline-library/java-serverless-pipeline/tests/unit.groovy"]
        def staticAnalysisTests = config?.staticAnalysisTests ?: [
                "${workspaceLibs}/pipeline-library/java-serverless-pipeline/tests/checkstyle.groovy",
                "${workspaceLibs}/pipeline-library/java-serverless-pipeline/tests/findbugs.groovy",
                "${workspaceLibs}/pipeline-library/java-serverless-pipeline/tests/owasp.groovy"
        ]
        def integrationTests = config?.integrationTests ?: ["${workspaceLibs}/pipeline-library/java-serverless-pipeline/tests/bdd.groovy"]
        def performanceTests = config?.performanceTests ?: ["${workspaceLibs}/pipeline-library/java-serverless-pipeline/tests/perf.groovy"]
        def vulnerabilityScanner = config?.vulnerabilityScanner ?: "${workspaceLibs}/pipeline-library/java-serverless-pipeline/imageVulnScan/vulnscanner.groovy"


        configurableServerlessStageHandlers = new ConfigurableServerlessStageHandlers(
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