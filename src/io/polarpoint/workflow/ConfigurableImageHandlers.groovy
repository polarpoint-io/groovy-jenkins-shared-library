/*
 * Surj Bains  <surj@polarpoint.io>
 * ConfigurableGenericHandlers
 */

package io.polarpoint.workflow


class ConfigurableImageHandlers implements Serializable {

    List<String> integrationTests
    String containerrun
    String builder
    String publisher


    public ConfigurableImageHandlers(
            List<String> integrationTests,
            String containerrun,
            String builder,
            String publisher)
    {
        this.integrationTests = integrationTests
        this.containerrun = containerrun
        this.builder = builder
        this.publisher = publisher


    }


}


