def build(String targetBranch, context) {



        utils = new io.polarpoint.utils.Utils()

        def artifactTag = utils.artifactTag(targetBranch) // use -RELEASE or -SNAPSHOT depending on the branch
        def artifact = utils.artifactName(artifactTag, context) //application-version-branch.jar
        def version = context.config.version
        def artifactId = context.config.archivesBaseName
        def group = context.config.group
        //def gradleHome = tool name: 'gradle-default', type: 'gradle'

        ansiColor('xterm') {
            //checkout scm
            try {
                echo "Build: about to call gradle with  branch ${targetBranch} artifact  ${artifact} artifactTag ${artifactTag}"

                withEnv([
                        "artifactName=${artifact}",
                        "artifactTag=${artifactTag}"
                ]) {
                    sh "gradle -PprojVersion=${version}-${artifactTag} -PprojName=${artifactId} -PprojGroup=${group} assemble"
                }



                stash name: 'build',
                        includes: "build/libs/${artifact}"


            } catch (err) {
                sh 'ls -la build/libs'
                error("build failed: " + err.message)



            } finally {
              //  cleanWs notFailBuild: true
            }
        }

}

def name() {
    return "build"
}

return this;
