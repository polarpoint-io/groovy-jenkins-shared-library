def publish(String targetBranch, context) {


    utils = new io.polarpoint.utils.Utils()
    def dockerName = utils.imageNameDocker(targetBranch,context)

    unstash 'node-artifacts'
    def exists = fileExists 'Dockerfile'
    if (exists) {
        try {
            def stagingDockerRegistryUrl =
                    context.config.stagingDockerRegistry?.url ?: "https://staging-image.pohzn.com"
            def stagingDockerRegistry = stagingDockerRegistryUrl.replaceAll("https://", "")
            def stagingImageLine = "${stagingDockerRegistry}/${dockerName}"
            def dockerRegistryUrl = context.config.dockerRegistry?.url
            def dockerRegistry = dockerRegistryUrl.replaceAll("https://", "")
            def imageLine = "${dockerRegistry}/${dockerName}"
            def credentialsID = context.config.dockerRegistry.credentialsID

            echo "Build: about to call docker retag and publish ${imageLine} "
            withCredentials([[$class          : 'UsernamePasswordMultiBinding',
                              credentialsId   : credentialsID,
                              usernameVariable: 'USER',
                              passwordVariable: 'PASSWORD']]) {
                sh """
                              docker login -u ${USER} -p ${PASSWORD} ${stagingDockerRegistry}
                              docker pull  ${stagingImageLine}
                              docker tag  ${stagingImageLine}  ${imageLine}

                              docker login -u ${USER} -p ${PASSWORD} ${dockerRegistry}
                              docker push ${imageLine}

                       """
            }
            stash name: 'artifacts',
                    includes: "**/pipelines/conf/configuration.json,Dockerfile,Jenkinsfile"


        } catch (err) {
            error("retag and publish failed: " + err.message)


        } finally {
            cleanWs notFailBuild: true
        }
    }
}

def name() {
    return "publisher"
}

return this;
