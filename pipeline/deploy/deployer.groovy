def deploy(String targetBranch, context) {

    echo "Starting deployment to Cloud foundry"
    utils = new io.polarpoint.utils.Utils()
    def url = 'https://api.uk-1.paas-cf.cloud.global.fujitsu.com'

    def organization = context.config.cloudFoundry.organization
    def credentials = context.config.cloudFoundry.credentials
    def cloudSpace = context.config.cloudFoundry.cloudSpace
    def memoryAllocation = context.config.cloudFoundry.memoryAllocation as int

    def artifactTag = utils.artifactTag(targetBranch)
    def version = context.config.version + "-" + artifactTag
    def artifact = utils.artifactName(artifactTag, context)
    def shortCFName = utils.artifactNameCloudFoundry(targetBranch)
    def appNameCloudFoundry = utils.appNameCloudFoundry(shortCFName, context)
    def fullArtifactPath = utils.fullArtifactPath(artifactTag, context)
    def artifactId = context.config.application
    def groupId = context.config.group
    def repository = utils.determineRepository(targetBranch, context)

    def clientCertCN = utils.clientCertCN('dev', context.config.archivesBaseName)


    node() {

        sleep(time:5)

        try {
            withMaven(globalMavenSettingsConfig: '', maven: 'mvn3.5.2') {
                withCredentials([
                        usernamePassword(credentialsId: 'svc-nexus-user', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                    retry(3) {
                        timeout(time: 10, unit: 'SECONDS') {
                            sh """
                         mvn dependency:get \
                        -DremoteRepositories=https://artifact.pohzn.com/repository/${repository} \
                        -DgroupId=${groupId} \
                        -DartifactId=${artifactId} \
                        -Dversion=${version} \
                        -Ddest=${fullArtifactPath}
                        """
                        }
                    }
                }
            }
        } catch (err) {
            error('Download of artifact from maven failed!')
        }

        try {
            // deploy if credentials exists - workaround for local jenkins
            if (utils.getCredentialsById(credentials)) {
                echo "fullArtifactPath $fullArtifactPath"
                echo "artifact $artifact"
                echo "appNameCloudFoundry ${appNameCloudFoundry}"

                withCredentials([string(credentialsId: clientCertCN, variable: 'keyStoreJson')]) {

                    def keyStoreMap = utils.parseJson(env.keyStoreJson)

                    pushToCloudFoundry(
                            target: "${url}",
                            organization: "${organization}",
                            cloudSpace: "${cloudSpace}",
                            credentialsId: "${credentials}",
                            selfSigned: true,
                            pluginTimeout: 240,
                            manifestChoice: [
                                    value  : 'jenkinsConfig',
                                    appName: appNameCloudFoundry,
                                    memory : memoryAllocation,
                                    envVars: [
                                            [key: "JAVA_OPTS", value: "-Dspring.profiles.active=dev"],
                                            [key: "ARTIFACT", value: "${artifact}"],
                                            [key: "VERSION", value: "${context.config.version}"],
                                            [key: "KEYSTORE", value: "${keyStoreMap.KEYSTORE}"],
                                            [key: "KEYSTORE_PASSWORD", value: "${keyStoreMap.KEYSTORE_PASSWORD}"],
                                    ],
                                    appPath: "${fullArtifactPath}"
                            ]
                    )
                }
            }
        } catch (err) {
            error('Push to cloud foundry failed ' + err.message)
        } finally {
            cleanWs notFailBuild: true
        }
    }
}


return this;
