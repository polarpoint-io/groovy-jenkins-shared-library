def deploy(String targetBranch, context) {

    echo "starting deployment to Cloud foundry"
    def url = context.config.cloudFoundry.url
    def organization = context.config.cloudFoundry.organization
    def appName = context.config.application
    def credentials = context.config.cloudFoundry.credentials
    def cloudSpace = context.config.cloudFoundry.cloudSpace
    def springCloudEnv = context.config.cloudFoundry.springCloudEnv
    def artifactId = context.config.archivesBaseName

    def memoryAllocation = context.config.cloudFoundry.memoryAllocation as String
    def environmentParameters = context.config.cloudFoundry.environmentParameters
    def appNameCloudFoundry = appName + '-dev'
    def pluginTimeout = "240" as String
    def signedCert = "false" as String

    node('master') {
        if (!fileExists('Dockerfile')) {

            if (utils.getCredentialsById(credentials)) {

                ansiColor('xterm') {
                    if (targetBranch =~ /development/) {

                        unstash 'package.json'
                        def props = readJSON file: 'package.json'
                        def version = props.version

                        try {
                            sh "curl  https://artifact.pohzn.com/repository/pol-npm/${artifactId}/-/${artifactId}-${version}.tgz | tar zxf -"
                        } catch (err) {
                            error('Couldn\'t get artifact from nexus!' + err.message)
                        }
                        // some node builds package everything into package/dist. No idea why!
                        dir('package/build') {


                            pushToCloudFoundry(
                                    target: "${url}",
                                    organization: "${organization}",
                                    cloudSpace: "${cloudSpace}",
                                    credentialsId: "${credentials}",
                                    selfSigned: signedCert,
                                    pluginTimeout: pluginTimeout,
                                    manifestChoice: [
                                            value  : 'jenkinsConfig',
                                            appName: appNameCloudFoundry,
                                            memory : memoryAllocation,
                                            envVars: [
                                                    [key: "JAVA_OPTS", value: "-Dspring.profiles.active=${springCloudEnv}"],
                                                    [key: "ARTIFACT", value: "${artifactId}"],
                                                    [key: "VERSION", value: "${version}"],
                                            ]
                                    ]
                            )
                        }
                    }
                }
            }
        }
    }


}


return this;

