/*
 * Surj Bains  <surj@polarpoint.io>
 * Javav_0_2_0ConfigurationContext
 */

package io.polarpoint.workflow.contexts

import io.polarpoint.workflow.handlers.ConfigurableJavav_0_2_0StageHandlers


@SuppressWarnings('FieldName')
class Javav_0_2_0ConfigurationContext implements Serializable {

    Javav_0_2_0ConfigurationContext() {
    }


    private static Pattern pattern = Pattern.compile("/(libs)[w./-]{1,253}\$(?<![./])")

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableJavav_0_2_0StageHandlers configurableStageHandlers

    Javav_0_2_0ConfigurationContext(String application, HashMap config) {
        this.application = application
        this.config = config

    }

    Javav_0_2_0ConfigurationContext(String application, String configuration, String ws = null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))

        def workspaceLibs = "${ws}@libs"

        // def dstPath = "${ws}@libs"+"/pipeline-library/"
        def utils = new io.polarpoint.utils.Utils()
        def matcher = pattern.matcher(utils.pc_lib_folder());
        def uuidPath =""
        if (matcher.find()) {
             println "**************FOUND REGEX :" +matcher.group(2)
             uuidPath= matcher.group(2)
        }
        workspaceLibs = uuidPath

        println "**********workspace LIB BROKEN PRESET1 *******  ${workspaceLibs}"

        def builder = config.builder ?: "${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/build/builder.groovy"
        def publisher = config?.publisher ?: "${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/publish/publisher.groovy"
        def deployer = config?.deployer ?: "${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/deploy/deployer.groovy"
        def containerBuilder = config?.containerBuilder ?: "${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/containerBuild/builder.groovy"
        def containerPublisher = config?.containerPublisher ?: "${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/containerPublish/publisher.groovy"
        def containerStager = config?.containerStager ?: "${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/containerStage/stager.groovy"
        def containerScanner = config?.containerScanner ?: "${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/containerScan/scanner.groovy"
        def qualityTests = config?.qualityTests ?: ["${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/tests/sonar.groovy"]
        def unitTests = config?.unitTests ?: ["${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/tests/unit.groovy"]
        def staticAnalysisTests = config?.staticAnalysisTests ?: [
                "${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/tests/checkstyle.groovy",
                "${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/tests/findbugs.groovy"
                // "${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/tests/owasp.groovy" disable owasp stage
        ]
        def integrationTests = config?.integrationTests ?: [
                "${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/tests/bdd.groovy",
                "${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/tests/integration.groovy"]
        def performanceTests = config?.performanceTests ?: ["${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/tests/performance.groovy"]
        def vulnerabilityScanner = config?.vulnerabilityScanner ?: "${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/imageVulnScan/vulnscanner.groovy"
        def chgLogger = config?.chgLogger ?: "${workspaceLibs}/pipeline-library/java-v-0-2-0-pipeline/chglog/chglogger.groovy"


        configurableStageHandlers = new ConfigurableJavav_0_2_0StageHandlers(
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
                vulnerabilityScanner,
                chgLogger
                )

    }



    String asJsonString() {

        return (new groovy.json.JsonBuilder(this.config)).toPrettyString()
    }
}
