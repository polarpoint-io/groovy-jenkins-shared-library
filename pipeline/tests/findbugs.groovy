/*
Run findbugs.
This won't be needed if sonar findbugs plugin is enabled

*/

def runTest(String targetBranch, context) {

    utils = new io.polarpoint.utils.Utils()

    def artifactId = context.config.archivesBaseName



       try {

            utils.printColour("Findbugs: about to call findbugs  ${targetBranch}", 'green')
            sh "gradle -q clean spotbugsMain"

        } catch (err) {
            echo(err.message)
            error("Findbugs failed:" + artifactId)


        } finally {
           findbugs canComputeNew: true, defaultEncoding: '', excludePattern: '', healthy: '', includePattern: '', pattern: 'build/reports/spotbugs/main.xml', unHealthy: ''
       }

    }


String name() {
    return "Findbugs"
}

return this;
