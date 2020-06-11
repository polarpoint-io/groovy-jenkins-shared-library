/*
Run License reporter.
*/

    def licenseReporter(String targetBranch, context) {

    utils = new io.polarpoint.utils.Utils()

    def artifactId = context.config.archivesBaseName
    podTemplate(label: 'gradle-6-0-1') {
        node('gradle-6-0-1') {
            container('gradle-6-0-1') {

                checkout scm
                try {

                    utils.printColour("About to call license Reporter  ${targetBranch}", 'green')
                    sh "gradle  -Dhttps.protocols=TLSv1.2 generateLicenseReport"


                } catch (err) {
                    error("generateLicenseReport failed: " + artifactId + err.message)


                } finally {
                    publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true,
                                 reportDir: 'build/reports/dependency-license/', reportFiles: 'report.html',
                                 reportName: 'License Report', reportTitles: ''])
                }
            }

            cleanWs()
        }
    }

}

String name() {
    return "License Reporter"
}

return this;

