/*
Run findbugs.
This won't be needed if sonar findbugs plugin is enabled

*/

def runTest(String targetBranch, context) {

    utils = new io.polarpoint.utils.Utils()

    def artifactId = context.config.archivesBaseName
    def coverage_exclusions = context.config.sonar.coverage_exclusions



                checkout scm

                try {
                    utils.printColour("Findbugs: about to call findbugs  ${targetBranch}", 'green')
                    sh "gradle -q spotbugsMain"


                } catch (err) {
                    echo(err.message)
                    error("Findbugs failed:" + artifactId)


                } finally {
                    findbugs defaultEncoding: '',
                            excludePattern: "${coverage_exclusions}",
                            pattern: 'build/reports/spotbugs/main.xml',
                            failedNewHigh: '10',
                            failedNewLow: '20',
                            failedNewNormal: '10',
                            failedTotalHigh: '10',
                            failedTotalLow: '20',
                            failedTotalNormal: '20',
                            unstableNewHigh: '5',
                            unstableNewLow: '10',
                            unstableNewNormal: '10',
                            unstableTotalHigh: '10',
                            unstableTotalLow: '20',
                            unstableTotalNormal: '20',


                            healthy: '',
                            includePattern: ''

                    stash name: 'findbugs-report', allowEmpty: true, includes: 'build/reports/spotbugs/main.xml'
                }

}


String name() {
    return "Findbugs"
}

return this;
