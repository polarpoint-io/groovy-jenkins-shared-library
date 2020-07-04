/*
Run checkstyle .
*/

def runTest(String targetBranch, context) {

    utils = new io.polarpoint.utils.Utils()

    def artifactId = context.config.archivesBaseName
    podTemplate(label: 'gradle-6') {
        node('gradle-6') {
            container('gradle-6') {

                checkout scm
                try {

                    utils.printColour("Checkstyle: about to call checkstyle  ${targetBranch}",
                            'green')
                    sh "gradle -q -Dhttps.protocols=TLSv1.2 checkstyleMain --parallel"


                } catch (err) {
                    //TODO uncomment below so builds fail on checkstyle
                    error("checkstyle failed: " + artifactId + err.message)


                } finally {
                    checkstyle defaultEncoding: '',
                            pattern: '**/checkstyle_output.xml',
                            healthy: '',
                            unHealthy: '',
                            unstableNewHigh: '10',
                            unstableNewLow: '1000',
                            unstableNewNormal: '1000',
                            unstableTotalHigh: '10',
                            unstableTotalLow: '1000',
                            unstableTotalNormal: '1000' //TODO all the configuration here needs to be per project
                }
            }

            cleanWs()
        }
    }

}

String name() {
    return "Checkstyle"
}

return this;
