def init(String targetBranch, context, Boolean toTag) {


    utils = new io.polarpoint.utils.Utils()

    ansiColor('xterm') {

        try {

            echo "Build: about to call helm package..."

            def application = context.config.application

            echo "Application: ${application}"

            dir("${application}") {
                checkout scm
                sh "pwd"
                sh "ls"
                sh "helm init -c"
                sh "helm lint ."

                if (toTag) {
                    invokeSemanticVersioning(targetBranch, context)
                }

                //sh "helm package ."
                archiveArtifacts artifacts: "*.tgz", onlyIfSuccessful: false //
                sh "ls"
            }

        } catch (err) {
            error("package failed: " + err.message)
            err.printStackTrace()


        } finally {
            cleanWs notFailBuild: true
        }

    }

}

def name() {
    return "init"
}

return this;
