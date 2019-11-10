def publish(String targetBranch, context) {


        utils = new io.polarpoint.utils.Utils()
    dir("publish")
                {

                    def tag = utils.artifactTag(targetBranch) // use -RELEASE or -SNAPSHOT depending on the branch
                    def artifact = utils.artifactNameZip(tag, context)
                    def nexusUrl = context.config.nexus.url
                    def repository = context.config.nexus.repository
                    def credentialsID = context.config.nexus.credentialsID
                    def version = context.config.version + '-' + tag
                    def groupId = context.config.group
                    def application = context.config.application
                    def nexus = nexusUrl+ repository


                  //  try {

                        unstash 'build'
//                        if (!fileExists("${artifact}")) {
//                            String synchroniseArtifact = sh(returnStdout: true, script: "mv *.zip ${artifact}")
//                            echo "synchroniseArtifact: synchroniseArtifact  ${synchroniseArtifact}"
//                        }
//                        stash name: 'artifacts',
//                                includes: "*.zip"
//                        archiveArtifacts artifacts: "*.zip",  onlyIfSuccessful: true
                         withCredentials([
                                string(credentialsId: credentialsID,
                                        variable: 'NEXUS_NUGET_KEY')
                        ]) {

                                echo "about to publish ${version} to ${nexus}"
                                 sh """
                                   nuget spec ${groupId}
                                   nuget setapikey ${NEXUS_NUGET_KEY} -source ${nexus}
                                    dotnet restore
                                   ls -la
                                    nuget pack  cma.nuspec -Version ${version}  -verbosity detailed
                                   dotnet nuget push build/libs/*.nupkg --source ${nexus} --api-key ${NEXUS_NUGET_KEY}
                                """







                        }
//                    } catch (err) {
//                        error("Failed to publish artifact to Nexus" + err.message)
//                    } finally {
//                        cleanWs notFailBuild: true
//                    }
                }

}

def name() {
    return "publisher"
}

return this;
