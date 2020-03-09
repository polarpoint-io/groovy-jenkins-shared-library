/*
 * Surj Bains  <surj@polarpoint.io>
 * ConfigurableNVDUpdateHandlers
 */

package io.polarpoint.workflow.handlers

class ConfigurableNVDUpdateHandlers implements Serializable, NVDUpdateHandlers {

    String report
    String apply



    public ConfigurableNVDUpdateHandlers(

            String report,
            String apply)
    {
        this.report = report
        this.apply = apply


    }


}


