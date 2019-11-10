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



                sh "curl -X POST -H 'Content-type: application/json' http://mgt-dagda-server:5000/v1/check/images/staging-image.pohzn.com/${dockerName}"
                sleep(time:15)
                sh " curl  -H 'Accept: application/json'  -H 'Content-type: application/json'  http://mgt-dagda-server:5000/v1/history/staging-image.pohzn.com/${dockerName}  | jq '.[].status'"


            } catch (err) {
                // allow to continue
               // error("build failed: " + err.message)


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
