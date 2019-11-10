/*
 * Surj Bains  <surj@polarpoint.io>
 * StageHandlers
 */

package io.polarpoint.workflow

interface StageHandlers {

    String getBuilder()
    String getDeployer()
    String getPublisher()
    List<String> getUnitTests()
    List<String> getQualityTests()
    List<String> getStaticAnalysisTests()
    List<String> getIntegrationTests()
    List<String> getPerformanceTests()
}

