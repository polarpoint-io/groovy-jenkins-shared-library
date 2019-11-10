/*
 * Surj Bains  <surj@polarpoint.io>
 * ConfigurableGenericHandlers
 */

package io.polarpoint.workflow


class ConfigurableGenericHandlers implements Serializable, GenericHandlers {

    List<String> qualityTests
    String deployer
    String builder
    String publisher


    public ConfigurableGenericHandlers(
            List<String> qualityTests,
            String deployer,
            String builder,
            String publisher)
    {
        this.qualityTests = qualityTests
        this.deployer = deployer
        this.builder = builder
        this.publisher = publisher

    }


}


