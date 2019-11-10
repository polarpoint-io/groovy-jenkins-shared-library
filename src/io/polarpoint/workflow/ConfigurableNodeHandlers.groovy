/*
 * Surj Bains  <surj@polarpoint.io>
 * ConfigurableGenericHandlers
 */

package io.polarpoint.workflow


class ConfigurableNodeHandlers {

    List<String> unitTests
    String deployer
    String builder
    String publisher
    String containerBuilder
    String containerPublisher
    String containerStager
    String containerScanner

    public ConfigurableNodeHandlers(
            List<String> unitTests,
            String deployer,
            String builder,
            String publisher,
            String containerBuilder,
            String containerPublisher,
            String containerStager,
            String containerScanner
    ) {
        this.unitTests = unitTests
        this.deployer = deployer
        this.builder = builder
        this.publisher = publisher
        this.containerBuilder = containerBuilder
        this.containerPublisher = containerPublisher
        this.containerStager = containerStager
        this.containerScanner = containerScanner
    }

    public ConfigurableNodeHandlers(
            List<String> unitTests,
            String deployer,
            String builder,
            String publisher,
            String containerBuilder,
            String containerPublisher
    ) {
        this.unitTests = unitTests
        this.deployer = deployer
        this.builder = builder
        this.publisher = publisher
        this.containerBuilder = containerBuilder
        this.containerPublisher = containerPublisher
    }



}


