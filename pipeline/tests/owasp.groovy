/*
Run checkstyle .
This won't be needed if sonar findbugs plugin is enabled
*/

def runTest(String targetBranch, context) {

    utils = new  io.polarpoint.utils.Utils()

    def artifactId = context.config.archivesBaseName


        try {

            utils.printColour("Checkstyle: about to call OWASP Dependency Checker ${targetBranch}", 'green')
            sh "gradle  dependencyCheckAnalyze"


        } catch (err) {
            error("OWASP failed: " + artifactId + err.message)


        } finally {

            publishHTML([allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true,
                         reportDir: 'build/reports', reportFiles: 'dependency-check-report.html',
                         reportName: 'OWASP Dependency Check Report', reportTitles: ''])
        }

    }



String name() {
    return "OWASP"
}

return this;
