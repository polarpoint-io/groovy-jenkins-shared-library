def deploy(String targetBranch, context) {

    echo "Starting deployment to apigee"
    utils = new io.polarpoint.utils.Utils()
    def credentialsID = context.config.nexus.credentials
    def artifactTag = utils.artifactTag(targetBranch) // use -RELEASE or -SNAPSHOT depending on the branch
    def apigeeCredentials = context.config.apigee.credentials
    def artifactName = "${context.config.application}-${context.config.version}-${artifactTag}"
    def repository = utils.determineRepository(targetBranch, context)


        configFileProvider(
                [configFile(fileId: 'Nexus', variable: 'MAVEN_SETTINGS')]) {
        withCredentials([
                usernamePassword(credentialsId: credentialsID, usernameVariable: "NEXUS_USER", passwordVariable: "NEXUS_PASS")]) {
            sh """
            mvn -s $MAVEN_SETTINGS dependency:get \
                        -DremoteRepositories=https://artifact.pohzn.com/repository/${repository} \
                        -DgroupId=${context.config.group} \
                        -DartifactId=${context.config.application} \
                        -Dversion=${context.config.version}-$artifactTag  \
                        -Ddest=${artifactName}.zip \
                        -Dpackaging=zip \

                 """
        }
        fileOperations(
                [
                        fileUnZipOperation(filePath: artifactName+".zip", targetLocation: artifactName)
                ]
        )

        withCredentials([
                usernamePassword(credentialsId: apigeeCredentials
                        , usernameVariable: 'APIGEE_USER', passwordVariable: 'APIGEE_PASS')]) {
            sh """
                cd ${artifactName}
                export APIGEE_OPTIONS=${context.config.apigee.options}
                export APIGEE_ORG=${context.config.apigee.org}
                export APIGEE_ENV=${context.config.apigee.env}
                export PROXY_DEPLOYMENT=${context.config.apigee.deployment}
                export PROXY_NAME=${context.config.apigee.proxy}
                export CF_DOMAIN=${context.config.apigee.cf.domain}
                export CF_CLIENT_AUTH=${context.config.apigee.cf.clientAuth.enabled}
                export CF_CLIENT_KEYSTORE=${context.config.apigee.cf.clientAuth.keyStore}
                export CF_CLIENT_KEYALIAS=${context.config.apigee.cf.clientAuth.keyAlias}
                export CF_CLIENT_TRUSTSTORE=${context.config.apigee.cf.clientAuth.trustStore}
                printenv
                mvn clean -X apigee-enterprise:deploy"""

        }
    }

}


return this;
