def apply(String targetBranch, context) {

    def url = context.config.rules.url
    def branch = context.config.rules.branch



            ansiColor('xterm') {

            try {

                echo "Build: about to apply "
                checkout scm


                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: '43cee12e-c6be-41e5-b5c3-79e3c47c1292', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASS']]) {

                    git url: "https://${GITHUB_USER}:${GITHUB_PASS}@${url}", branch: "${branch}", credentialsId: '43cee12e-c6be-41e5-b5c3-79e3c47c1292'
                    sh 'git config --global user.email \"jenkins@pohzn.com\"'
                    sh 'git config --global user.name \"Jenkins Server\"'
                    merge = sh(returnStdout: true, script: "git merge -s ours origin/master")
                    merge = sh(returnStdout: true, script: "git checkout master")
                    configCommit = sh(returnStdout: true, script: "git merge ${branch} ")
                    push = sh(returnStdout: true, script: "git push https://${GITHUB_USER}:${GITHUB_PASS}@${url}  ")
                    
                }

            } catch (err) {
                error("apply failed: " + err.message)


            } finally {
                cleanWs notFailBuild: true
            }

        }

}

def name() {
    return "applier"
}

return this;
