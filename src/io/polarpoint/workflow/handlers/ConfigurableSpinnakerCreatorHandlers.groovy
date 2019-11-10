/*
 * Surj Bains  <surj@polarpoint.io>
 * ConfigurableSpinnakerSyncHandlers
 */

package io.polarpoint.workflow.handlers

class ConfigurableSpinnakerCreatorHandlers implements Serializable, SpinnakerSyncHandlers {

    String init
    String apply



    public ConfigurableSpinnakerCreatorHandlers(

            String init,
            String apply)
    {
        this.init = init
        this.apply = apply


    }


}


