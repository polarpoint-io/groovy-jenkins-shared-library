def runTest(String targetBranch, context) {

    utils = new io.polarpoint.utils.Utils()



        try {
            def url = context.config.bdd.url
            def branch = context.config.bdd.branch

            def testSuite= context.config.bdd.test_suite
            def hngtEnvironment= context.config.bdd.hngt_environment
            def cert_file_name = context.config.bdd.nonintCertfile

            // If no value provided, then use default value from config file
            if (!testSuite?.trim()) {
                echo "Warning - No value provided for TEST_SUITE so defaulting to " + context.config.bdd.test_suite
                testSuite = ""
            } else {
                echo "Debug - test_suite value specified"
                testSuite = "-Dtags="+testSuite
            }
            if (!hngtEnvironment?.trim()) {
                echo "Warning - No value provided for HNGT_ENVIRONMENT so defaulting to " + context.config.bdd.hngt_environment
                hngtEnvironment = context.config.bdd.hngt_environment
            } else {
                echo "Debug - hngt_environment value specified"
            }

            if (hngtEnvironment=="int") {
                cert_file_name = context.config.bdd.intCertfile;
            }


            withEnv(["HNGT_ENVIRONMENT=${hngtEnvironment}"]) {

                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: '43cee12e-c6be-41e5-b5c3-79e3c47c1292', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASS']]) {
                    dir('bdd') {
                        timestamps {
                                            git url: "https://${GITHUB_USER}:${GITHUB_PASS}@${url}", branch: "${branch}", credentialsId: '43cee12e-c6be-41e5-b5c3-79e3c47c1292'
                                            sh "id"
                                            sh "gradle clean -Denv=${hngtEnvironment} -Dcert=${cert_file_name} $testSuite test aggregate"

                        }

                    }

                }
            }
        } catch (err) {
            error("Integration tests failed: "  + err.message)


        } finally {
            publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true,
                         reportDir: 'target/site/serenity', reportFiles: 'index.html',
                         reportName: 'Serenity Report', reportTitles: ''])
        }

}



String name() {
    return "BDD"
}

return this;
