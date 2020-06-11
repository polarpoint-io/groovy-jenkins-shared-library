/*
Run OWASP .

*/

def runTest(String targetBranch, context) {


        utils = new io.polarpoint.utils.Utils()

        def artifactId = context.config.archivesBaseName

    podTemplate(label: 'owasp') {
        node('owasp') {
            container('owasp') {

                checkout scm
                 try {
                utils.printColour("OWASP: about to call OWASP Dependency Checker ${targetBranch}", 'green')
                sh('mkdir -p build/owasp')
                dependencyCheck additionalArguments: '''--scan ./
                                --format XML
                                --data /mnt/nvd/
                                --noupdate
                                --out build/owasp/dependency-check-report.xml
                                ''', odcInstallation: '5.3.2'
               } catch (err) {
                    error("OWASP failed: " + artifactId + err.message)

                } finally {
                
                dependencyCheckPublisher(
                        canComputeNew: false,
                        defaultEncoding: '',
                        failedTotalAll: '2', // fail if greater than 3 vulns
                        failedTotalHigh: '0', // fail if any high vulns
                        healthy: '',
                        pattern: 'build/owasp/dependency-check-report.xml',
                        unHealthy: '2' //build is unhealthy while there are more than 2 vulns
                )

                }
                cleanWs()
            }
        }
    }

}



String name() {
    return "OWASP"
}

return this;
