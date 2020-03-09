def build(String targetBranch, context) {


    utils = new io.polarpoint.utils.Utils()
    def dockerName = utils.imageNameDocker(targetBranch,context)

    unstash 'node-artifacts'
    ansiColor('xterm') {
        def exists = fileExists 'Dockerfile'
        if (exists) {

            try {
                def stagingDockerRegistryUrl =
                        context.config.stagingDockerRegistry?.url ?: "https://staging-image.pohzn.com"
                def stagingDockerRegistry = stagingDockerRegistryUrl.replaceAll("https://", "")
                def imageLine = "${stagingDockerRegistry}/${dockerName}"
                def credentialsID = context.config.dockerRegistry.credentialsID

                echo "Build: about to call docker build  ${imageLine} "
                withCredentials([[$class          : 'UsernamePasswordMultiBinding',
                                  credentialsId   : credentialsID,
                                  usernameVariable: 'USER',
                                  passwordVariable: 'PASSWORD']]) {
                    sh """
                              docker login -u ${USER} -p ${PASSWORD} ${stagingDockerRegistry}
                       """
                }

                appContainer = docker.build("${imageLine}")


            } catch (err) {
                error("build failed: " + err.message)


            } finally {
                cleanWs notFailBuild: true
            }
        } else {
            echo("[Docker Container Build]")
            echo "*** No Dockerfile can't build ***"
        }
    }

}

def name() {
    return "build"
}

return this;
