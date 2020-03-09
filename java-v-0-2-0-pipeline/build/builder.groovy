def build(String targetBranch, context) {


    utils = new io.polarpoint.utils.Utils()

    def artifactTag = utils.artifactTag(targetBranch) // use -RELEASE or -SNAPSHOT // depending on the branch
    def artifact = utils.artifactName(artifactTag, context) //application-version-branch.jar
    def version = context.config.version
    def artifactId = context.config.archivesBaseName
    def group = context.config.group

    ansiColor('xterm') {

        if (artifactId != context.config.application) {
            utils.printColour("red","archivesBaseName (" + artifactId + ") doesn't match application (" + context.config.application + ")  in configuration.json")
        }

        try {
            echo """Build: about to call gradle with 
            branch ${targetBranch} 
            artifact  ${artifact} 
            artifactTag ${artifactTag}"""

            withEnv([
                    "artifactName=${artifact}",
                    "artifactTag=${artifactTag}"
            ]) {

                    sh "gradle -PprojVersion=${version}-${artifactTag} -PprojName=${artifactId} -PprojGroup=${group} assemble --stacktrace "

            }

            stash name: 'build',
                    includes: "build/libs/*,**/pipelines/**,Dockerfile,Jenkinsfile,pom.xml"

        } catch (err) {
            sh 'ls -la build/libs'
            error("build failed: " + err.message)
        }
    }

}

def name() {
    return "build"
}

return this;
