/*
Run checkstyle .
This won't be needed if sonar findbugs plugin is enabled
*/

def runTest(String targetBranch, context) {

    utils = new
            io.polarpoint.utils.Utils()

    def artifactId = context.config.archivesBaseName


        try {

            utils.printColour("Checkstyle: about to call checkstyle  ${targetBranch}", 'green')
            sh "gradle -q clean checkstyleMain"


        } catch (err) {
            //TODO uncomment below so builds fail on checkstyle
            //error("checkstyle failed: " + artifactId + err.message)


        } finally {

            step([$class         : "CheckStylePublisher",
                  canComputeNew  : false,
                  defaultEncoding: "",
                  healthy        : "",
                  pattern        : "**/checkstyle/checkstyle_output.xml",
                  unHealthy      : ""])
        }

    }



String name() {
    return "Checkstyle"
}

return this;
