/*
 * Surj Bains  <surj@polarpoint.io>
 * DotNetContext
 */

package io.polarpoint.workflow.contexts
import io.polarpoint.workflow.handlers.ConfigurableDotNetStageHandlers


@SuppressWarnings('FieldName')
class DotNetContext implements Serializable {

    DotNetContext() {
    }

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableDotNetStageHandlers configurableDotNetStageHandlers

    DotNetContext(String application, HashMap config) {
        this.application = application
        this.config = config

    }

    DotNetContext(String application, String configuration, String ws = null) {
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

        def builder = config.builder ?: "${workspaceLibs}/pipeline-library/dotnet-pipeline/build/builder.groovy"
        def publisher = config?.publisher ?: "${workspaceLibs}/pipeline-library/dotnet-pipeline/publish/publisher.groovy"
        def deployer = config?.deployer ?: "${workspaceLibs}/pipeline-library/dotnet-pipeline/deploy/deployer.groovy"
        def containerBuilder = config?.containerBuilder ?: "${workspaceLibs}/pipeline-library/dotnet-pipeline/containerBuild/builder.groovy"
        def containerPublisher = config?.containerPublisher ?: "${workspaceLibs}/pipeline-library/dotnet-pipeline/containerPublish/publisher.groovy"
        def containerStager = config?.containerStager ?: "${workspaceLibs}/pipeline-library/dotnet-pipeline/containerStage/stager.groovy"
        def containerScanner = config?.containerScanner ?: "${workspaceLibs}/pipeline-library/dotnet-pipeline/containerScan/scanner.groovy"
        def qualityTests = config?.qualityTests ?: ["${workspaceLibs}/pipeline-library/dotnet-pipeline/tests/sonar.groovy"]
        def unitTests = config?.unitTests ?: ["${workspaceLibs}/pipeline-library/dotnet-pipeline/tests/unit.groovy"]
        def staticAnalysisTests = config?.staticAnalysisTests ?: [
                "${workspaceLibs}/pipeline-library/dotnet-pipeline/tests/checkstyle.groovy",
                "${workspaceLibs}/pipeline-library/dotnet-pipeline/tests/findbugs.groovy",
                "${workspaceLibs}/pipeline-library/dotnet-pipeline/tests/owasp.groovy"
        ]
        def integrationTests = config?.integrationTests ?: ["${workspaceLibs}/pipeline-library/dotnet-pipeline/tests/bdd.groovy"]
        def performanceTests = config?.performanceTests ?: ["${workspaceLibs}/pipeline-library/dotnet-pipeline/tests/perf.groovy"]
        def vulnerabilityScanner = config?.vulnerabilityScanner ?: "${workspaceLibs}/pipeline-library/dotnet-pipeline/imageVulnScan/vulnscanner.groovy"


        configurableDotNetStageHandlers = new ConfigurableDotNetStageHandlers(
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