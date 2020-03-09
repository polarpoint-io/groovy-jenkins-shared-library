def report(String targetBranch, context) {


    utils = new io.polarpoint.utils.Utils()

    ansiColor('xterm') {

        def url = context.config.url
        def branch = context.config.branch

        ansiColor('xterm') {

            try {

                echo "Build: about to push to GIT "

                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: '43cee12e-c6be-41e5-b5c3-79e3c47c1292', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASS']]) {

                    git url: "https://${GITHUB_USER}:${GITHUB_PASS}@${url}", branch: "${branch}", credentialsId: '43cee12e-c6be-41e5-b5c3-79e3c47c1292'
                    sh 'git config --global user.email \"jenkins@pohzn.com\"'
                    sh 'git config --global user.name \"Jenkins Server\"'
                    merge = sh(returnStdout: true, script: "git checkout master")
                    merge = sh(returnStdout: true, script: "cp /grafana_db/1/dashboards/*json dashboards/")
                    configCommit = sh(returnStdout: true, script: "git add *")
                    configCommit = sh(returnStdout: true, script: "git commit -m \"adding json exported\"")
                    push = sh(returnStdout: true, script: "git push https://${GITHUB_USER}:${GITHUB_PASS}@${url}  ")

                }

            } catch (err) {
                error("apply failed: " + err.message)


            } finally {
                cleanWs notFailBuild: true
            }

        }

    }

}

def name() {
    return "reporter"
}

return this;
