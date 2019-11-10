/*
 * Surj Bains  <surj@polarpoint.io>
 * ConfigurablePrometheusConfigHandlers
 */

package io.polarpoint.workflow.handlers

class ConfigurablePrometheusConfigHandlers implements Serializable, PrometheusConfigHandlers {

    String validate
    String apply



    public ConfigurablePrometheusConfigHandlers(

            String validate,
            String apply)
    {
        this.validate = validate
        this.apply = apply


    }


}


