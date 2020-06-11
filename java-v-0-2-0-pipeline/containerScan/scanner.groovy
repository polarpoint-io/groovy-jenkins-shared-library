def scan(String targetBranch, context) {

    utils = new io.polarpoint.utils.Utils()

    ansiColor('xterm') {
        checkout scm
        def exists = fileExists 'Dockerfile'
        if (exists) {

            try {
                def dockerName = utils.imageNameDocker(targetBranch,context)
                def stagingDockerRegistryUrl =
                        context.config.stagingDockerRegistry?.url ?: "https://staging-image.pohzn.com"
                def stagingDockerRegistry = stagingDockerRegistryUrl.replaceAll("https://", "")
                def imageLine = "${stagingDockerRegistry}/${dockerName}"


                writeFile file: 'anchore_images', text: imageLine

                anchore name: 'anchore_images', bailOnFail: true, bailOnPluginFail: false, engineRetries: '900'

                //uncommentin temporarily scaning


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
    return "scanner"
}

return this;
