def stage(String targetBranch, context) {


    utils = new io.polarpoint.utils.Utils()

    checkout scm
    def exists = fileExists 'Dockerfile'
    if (exists) {
        try {
            def dockerName = utils.imageNameDocker(targetBranch,context)
            def stagingDockerRegistryUrl =
                    context.config.stagingDockerRegistry?.url ?: "https://staging-image.pohzn.com"

            def stagingDockerRegistry = stagingDockerRegistryUrl.replaceAll("https://", "")
            def imageLine = "${stagingDockerRegistry}/${dockerName}"
            echo "Build: about to call docker publish to staging repository ${imageLine}"
            appContainer.push()



        } catch (err) {
            println("Failed to publish to staging repository" + err.message)
        } finally {
            cleanWs notFailBuild: true
        }
    } else {
        echo("[Docker Container Publish]")
        echo "*** No Dockerfile can't publish ***"
    }

}

def name() {
    return "publisher"
}

return this;
