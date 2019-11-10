/*
 * Surj Bains  <surj@polarpoint.io>
 * TerraformHandlers an interface for Docker build stages
 */

package io.polarpoint.workflow.handlers

interface SpinnakerSyncHandlers {

    String getInit()
    String getApply()

}

