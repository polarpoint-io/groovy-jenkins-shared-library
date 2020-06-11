def apply(String targetBranch, context) {

    def url = context.config.github.url
    def user = context.config.github.user



            ansiColor('xterm') {

            try {

                echo "Build: about to run git rob "
                checkout scm


                        withCredentials([string(credentialsId: 'git-rob-token', variable: 'GITROB_ACCESS_TOKEN')]){

                             sh """export FONTCONFIG_PATH=/etc/fonts; export GITROB_ACCESS_TOKEN=$GITROB_ACCESS_TOKEN ;  gitrob -enterprise-user=$user -enterprise-url=$url  -save gitrob-session.json hih dev-ops-common """

                        }
                stash name: 'apply',
                        includes: "gitrob-session.json, report.png, index.html"

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
