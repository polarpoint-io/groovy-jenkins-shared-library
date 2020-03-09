/*
Run Integration Tests.
*/

def runTest(String targetBranch, context) {

    utils = new io.polarpoint.utils.Utils()

    def artifactId = context.config.archivesBaseName
    podTemplate(label: 'gradle-6-0-1') {
        node('gradle-6-0-1') {
            container('gradle-6-0-1') {

                checkout scm
                try {

                    utils.printColour("Integration Tests about to call integration tests  ${targetBranch}", 'green')
                    sh "gradle  -Dhttps.protocols=TLSv1.2 integrationTest"


                } catch (err) {
                    error("integrationTest failed: " + artifactId + err.message)


                } finally {
                    junit allowEmptyResults: true,  '**/build/test-results/integrationTest/*.xml'
                }
            }

            cleanWs()
        }
    }

}

String name() {
    return "IntegrationTests"
}

return this;

