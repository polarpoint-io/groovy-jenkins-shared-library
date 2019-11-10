/*
 * Surj Bains  <surj@polarpoint.io>
 * ConfigurableGenericHandlers
 */

package io.polarpoint.workflow


class ConfigurableDockerHandlers implements Serializable, DockerHandlers {

    List<String> qualityTests
    String scanner
    String builder
    String stager
    String publisher


    public ConfigurableDockerHandlers(
            List<String> qualityTests,
            String scanner,
            String builder,
            String stager,
            String publisher)
    {
        this.qualityTests = qualityTests
        this.scanner = scanner
        this.builder = builder
        this.stager = stager
        this.publisher = publisher

    }


}


