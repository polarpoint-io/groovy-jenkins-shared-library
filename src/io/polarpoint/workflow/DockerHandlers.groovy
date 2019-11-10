/*
 * Surj Bains  <surj@polarpoint.io>
 * DockerHandlers an interface for Docker build stages
 */

package io.polarpoint.workflow

interface DockerHandlers {

    String getScanner()
    String getStager()
    List<String> getQualityTests()
    String getBuilder()
    String getPublisher()
}

