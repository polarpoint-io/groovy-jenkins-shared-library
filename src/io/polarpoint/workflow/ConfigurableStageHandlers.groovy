/*
 * Surj Bains  <surj@polarpoint.io>
 * ConfigurableStageHandlers
 */

package io.polarpoint.workflow


class ConfigurableStageHandlers implements Serializable, StageHandlers {

    String semanticVersioner
    String builder
    String publisher
    String containerBuilder
    String containerStager
    String containerScanner
    String containerPublisher
    String deployer
    String containerDeployer
    List<String> unitTests
    List<String> qualityTests
    List<String> staticAnalysisTests
    List<String> integrationTests
    List<String> performanceTests
    String vulnerabilityScanner


    public ConfigurableStageHandlers(
            String semanticVersioner,
            String builder,
            String publisher,
            String containerBuilder,
            String containerStager,
            String containerScanner,
            String containerPublisher,
            String deployer,
            List<String> qualityTests,
            List<String> unitTests,
            List<String> staticAnalysisTests,
            List<String> integrationTests,
            List<String> performanceTests,
            String vulnerabilityScanner) {

        this.semanticVersioner = semanticVersioner
        this.builder = builder
        this.publisher = publisher
        this.containerBuilder = containerBuilder
        this.containerStager = containerStager
        this.containerScanner = containerScanner
        this.containerPublisher= containerPublisher
        this.deployer = deployer
        this.qualityTests = qualityTests
        this.unitTests = unitTests
        this.staticAnalysisTests = staticAnalysisTests
        this.integrationTests = integrationTests
        this.performanceTests = performanceTests
        this.vulnerabilityScanner = vulnerabilityScanner

    }


}