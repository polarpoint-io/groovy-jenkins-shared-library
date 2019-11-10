def runcontainer(String targetBranch, context) {


    utils = new io.polarpoint.utils.Utils()
    def tag = utils.artifactTag(targetBranch) // use -RELEASE or -SNAPSHOT depending on the branch
    def artifact = utils.artifactName(tag, context) //application-version-branch.jar
    def artifactId = context.config.application
    def nexusUrl = 'https://image.pohzn.com:443'
    def credentialsID = 'svc-nexus-user'
    def groupId = context.config.group
    def version = context.config.version + "-" + tag
    def dockerRegistry = context.config.nexus.dockerRegistry
    def repository = utils.determineRepository(targetBranch, context)


    try {
        echo "Build: about to call docker build  ${artifactId} ${version}"
        withCredentials([[$class: 'UsernamePasswordMultiBinding',
                          credentialsId: credentialsID,
                          usernameVariable: 'USER',
                          passwordVariable: 'PASSWORD']]) {
            sh """
                          docker login -u ${USER} -p ${PASSWORD} ${dockerRegistry}
                    """
        }
        docker.withRegistry(nexusUrl, credentialsID) {
            //appContainer=  appContainer.pull();
        }




    } catch (err) {
        error("Failed to run image artifact from Nexus" + err.message)
    } finally {
        cleanWs notFailBuild: true
    }

}

def name() {
    return "container"
}

return this;
