/*
 * Surj Bains  <surj@polarpoint.io>
 * ConfigurableSpinnakerSyncHandlers
 */

package io.polarpoint.workflow.handlers

class ConfigurableSpinnakerSyncHandlers implements Serializable, SpinnakerSyncHandlers {

    String init
    String apply



    public ConfigurableSpinnakerSyncHandlers(

            String init,
            String apply)
    {
        this.init = init
        this.apply = apply


    }


}


