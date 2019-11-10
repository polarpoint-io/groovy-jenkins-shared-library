def runTest(String targetBranch, context) {

    utils = new io.polarpoint.utils.Utils()
    def server_id = context.config.sonar.server_id
    def artifactId = context.config.archivesBaseName
    def group = context.config.group
    def project_name = artifactId + "-" + targetBranch.replace('/', '-')
    def project_key = group + ":" + artifactId + "-" + targetBranch.replace('/', '-')
    def artifactTag = utils.artifactTag(targetBranch)
    def version = context.config.version + "-" + artifactTag
    def exclusions = context.config.sonar.exclusions
    def coverage_exclusions = context.config.sonar.coverage_exclusions
    def server_base = context.config.sonar?.server_base
    def server_base_url=""

    if (server_base !=null)
    {
        server_base_url =  " -Dsonar.core.serverBaseURL=${server_base}"
    }
    podTemplate(label: 'jnlp') {
        node('jnlp') {
            container('gradle') {
                try {

                    checkout scm
                    echo "Sonar: about to call sonar ${targetBranch}"
                    withSonarQubeEnv("${server_id}") {
                        echo "about to call sonar with  ${artifactId}"
                        sh """
                gradle \
                --info  sonarqube  \
                -Dsonar.projectKey=${project_key} \
                -Dsonar.projectName=${project_name} \
                -Dsonar.projectVersion=${version} \
                -Dsonar.exclusions=${exclusions} \
                -Dsonar.coverage.exclusions=${coverage_exclusions}  ${server_base_url}
                """
                    }

                    stash(name: 'workspace', useDefaultExcludes: false)

                } catch (err) {
                    echo(err.message)
                    error("sonar failed:" + artifactId)


                } finally {
                    junit allowEmptyResults: true, testResults: '**/TEST*.xml'


                }
            }
            cleanWs()
        }
    }
}


String name() {
    return "Sonar"
}

return this;
