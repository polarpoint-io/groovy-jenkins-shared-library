/*
 * Surj Bains  <surj@polarpoint.io>
 * ConfigurableHelmHandlers
 */

package io.polarpoint.workflow.handlers

import io.polarpoint.workflow.handlers.HelmHandlers


class ConfigurableHelmHandlers implements Serializable, HelmHandlers {

    String packager


    public ConfigurableHelmHandlers(
            String packager)
    {
        this.packager = packager

    }

}


