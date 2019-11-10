/*
 * Surj Bains  <surj@polarpoint.io>
 * TerraformHandlers an interface for Docker build stages
 */

package io.polarpoint.workflow.handlers

interface TerraformHandlers {

    String getInit()
    String getPlan()
    String getVisual()
    String getApply()

}

