def publish(String targetBranch, context) {


        utils = new io.polarpoint.utils.Utils()
        def tag = utils.artifactTag(targetBranch) // use -RELEASE or -SNAPSHOT depending on the branch
        def artifact = utils.artifactName(tag, context) //application-version-branch.jar
        def artifactId = context.config.application
        def nexusUrl = 'artifact.pohzn.com'
        def credentialsID = 'svc-nexus-user'
        def groupId = context.config.group
        def version = context.config.version + "-" + tag
        def repository = utils.determineRepository(targetBranch, context)


      try {

            unstash 'build'

            sh ' find . -ls'
            if (!fileExists("build/libs/${artifact}")) {
                jarToMove = sh (script: 'ls build/libs | egrep  "(FINAL|RELEASE|SNAPSHOT).jar$"', returnStdout: true).trim()
                echo "Move ${jarToMove} to build/libs/${artifact}"
                String synchroniseArtifact = sh(returnStdout: true, script: "mv build/libs/${jarToMove} build/libs/${artifact}")
                echo "synchroniseArtifact: synchroniseArtifact  ${synchroniseArtifact}"
            }

            stash name: 'artifacts',
                    includes: "build/libs/*.jar,**/pipelines/**,Dockerfile,Jenkinsfile,pom.xml"
            archiveArtifacts artifacts: "build/libs/*.jar",
                    onlyIfSuccessful: true


            echo "Build: about to call nexus upload with  branch ${targetBranch} artifact  ${artifact} artifactTag ${tag}"

            sh 'rm -rf target/artifact'
            dir('target/artifact') {
                unstash 'artifacts'

                withCredentials([
                        usernameColonPassword(credentialsId: credentialsID,
                                variable: 'NEXUS_CREDS')
                ]) {
                    nexusArtifactUploader(
                            nexusVersion: 'nexus3',
                            protocol: 'https',
                            nexusUrl: nexusUrl,
                            groupId: groupId,
                            version: version,
                            repository: repository,
                            credentialsId: credentialsID,
                            artifacts: [
                                    [artifactId: artifactId, file: "build/libs/${artifact}", type: 'jar']
                            ]
                    )
                }
            }

        } catch (err) {
            error("Failed to publish artifact to Nexus" + err.message)
        } finally {
            cleanWs notFailBuild: true
        }

}

def name() {
    return "publisher"
}

return this;
