def build(String targetBranch, context) {


    utils = new io.polarpoint.utils.Utils()

    unstash 'artifacts'
    sh 'find .'
    ansiColor('xterm') {
        try {

            def dockerName = utils.imageNameDocker(targetBranch, context)
            def dockerRegistryUrl = context.config.dockerRegistry.url
            def credentialsID = context.config.dockerRegistry.credentialsID
            def dockerRegistry = dockerRegistryUrl.replaceAll("https://", "")
            def stagingDockerRegistryUrl = context.config.stagingDockerRegistry?.url
            def stagingDockerRegistry = stagingDockerRegistryUrl.replaceAll("https://", "")
            echo "new docker image app names :[" + dockerName + "]"
            def imageLine = "${stagingDockerRegistry}/${dockerName}"
            echo "Build: about to call docker build  ${imageLine} "
            withCredentials([[$class          : 'UsernamePasswordMultiBinding',
                              credentialsId   : credentialsID,
                              usernameVariable: 'USER',
                              passwordVariable: 'PASSWORD']]) {
                sh "docker login -u ${USER} -p ${PASSWORD} ${stagingDockerRegistry}"
            }

            appContainer = docker.build("${imageLine}")

        } catch (err) {
            error("build failed: " + err.message)
        } finally {
            cleanWs notFailBuild: true
        }

    }
}

def name() {
    return "build"
}

return this;
