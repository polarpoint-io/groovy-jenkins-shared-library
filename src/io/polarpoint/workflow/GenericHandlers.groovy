/*
 * Surj Bains  <surj@polarpoint.io>
 * GenericHandlers
 */

package io.polarpoint.workflow

 interface GenericHandlers {

	 String getDeployer()
     List<String> getQualityTests()
     String getBuilder()
     String getPublisher()
}

