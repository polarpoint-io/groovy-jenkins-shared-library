/*
 * Surj Bains  <surj@polarpoint.io>
 * ConfigurablePrometheusConfigHandlers
 */

package io.polarpoint.workflow.handlers

class ConfigurablePowershellHandlers implements Serializable, PowershellHandlers {

    String report
    String apply



    public ConfigurablePowershellHandlers(

            String report,
            String apply)
    {
        this.report = report
        this.apply = apply


    }


}