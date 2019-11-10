/*
 * Surj Bains  <surj@polarpoint.io>
 * PrometheusConfigHandlers an interface for Prometheus Alerts and Config
 */

package io.polarpoint.workflow.handlers

interface PrometheusConfigHandlers {

    String getValidate()
    String getApply()

}

