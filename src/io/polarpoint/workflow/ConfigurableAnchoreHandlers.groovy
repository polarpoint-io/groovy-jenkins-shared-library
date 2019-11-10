/*
 * Surj Bains  <surj@polarpoint.io>
 * ConfigurableGenericHandlers
 */

package io.polarpoint.workflow


class ConfigurableAnchoreHandlers implements Serializable, AnchoreHandlers {

    List<String> qualityTests



    public ConfigurableAnchoreHandlers(
            List<String> qualityTests
)
    {
        this.qualityTests = qualityTests


    }


}


