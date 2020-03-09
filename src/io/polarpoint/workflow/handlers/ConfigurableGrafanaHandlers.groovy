/*
 * Surj Bains  <surj@polarpoint.io>
 * ConfigurableNVDUpdateHandlers
 */

package io.polarpoint.workflow.handlers

class ConfigurableGrafanaHandlers implements Serializable, GrafanaHandlers {

    String report
    String apply



    public ConfigurableGrafanaHandlers(

            String report,
            String apply)
    {
        this.report = report
        this.apply = apply


    }


}


