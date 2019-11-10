/*
 * Surj Bains  <surj@polarpoint.io>
 * ConfigurableTerraformHandlers
 */

package io.polarpoint.workflow.handlers

import io.polarpoint.workflow.handlers.TerraformHandlers


class ConfigurableTerraformHandlers implements Serializable, TerraformHandlers {

    String init
    String plan
    String visual
    String apply



    public ConfigurableTerraformHandlers(

            String init,
            String plan,
            String visual,
            String apply)
    {
        this.init = init
        this.plan = plan
        this.visual = visual
        this.apply = apply


    }


}


