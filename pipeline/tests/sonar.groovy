def runTest(String targetBranch, context) {

    utils = new io.polarpoint.utils.Utils()
    def server_id = context.config.sonar.server_id
    def artifactId = context.config.archivesBaseName
    def group = context.config.group
    def project_name = artifactId + "-" + targetBranch
    def project_key = group + ":" + artifactId
    def artifactTag = utils.artifactTag(targetBranch)
    def version = context.config.version + "-" + artifactTag
    def exclusions = context.config.sonar.exclusions
    def coverage_exclusions  = context.config.sonar.coverage_exclusions

        try {

            echo "Sonar: about to call sonar ${targetBranch}"

            withSonarQubeEnv("${server_id}") {
                echo "about to call sonar with  ${artifactId}"
                sh """
                gradle \
                --info clean sonarqube  \
                -Dsonar.projectName=${project_name}  \
                -Dsonar.projectKey=${artifactId} \
                -Dsonar.projectVersion=${version} \
                -Dsonar.exclusions=${exclusions} \
                -Dsonar.coverage.exclusions=${coverage_exclusions}
                """
            }

            stage("Quality Gate") {
                timeout(time: 1, unit: 'HOURS') {
                    def qg = waitForQualityGate()
                    if (qg.status != 'OK') {
                        error "Pipeline aborted due to quality gate failure: ${qg.status}"
                    }
                }
            }


            stash(name: 'workspace', useDefaultExcludes: false)

        } catch (err) {
            echo(err.message)
            error("sonar failed:" + artifactId)


        } finally {
            junit allowEmptyResults: true, testResults: '**/TEST*.xml'


        }
    }



String name() {
    return "Sonar"
}

return this;
