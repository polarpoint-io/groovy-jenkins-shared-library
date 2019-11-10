def validate(String targetBranch, context) {

    def promToolPod = context.config.promToolPod
    def url = context.config.rules.url
    def branch = context.config.rules.branch

    ansiColor('xterm') {
        checkout scm

            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: '43cee12e-c6be-41e5-b5c3-79e3c47c1292', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASS']]) {
                    try {
                        git url: "https://${GITHUB_USER}:${GITHUB_PASS}@${url}", branch: "${branch}", credentialsId: '43cee12e-c6be-41e5-b5c3-79e3c47c1292'
                        sh "tree ."
                        def statusCode = sh script: "/app/amtool check-config **/*.yaml", returnStatus:true

                        if (statusCode!=0)
                                {
                                    error "Alert Manager configuration check failed."
                                }
                    }
                    catch (error) {
                        println error.message
                    }
                    finally {

                    }


            }
    }

}

def name() {
    return "validator"
}

return this;
