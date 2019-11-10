def apply(String targetBranch, context) {




            ansiColor('xterm') {

            try {

                echo "Build: about to apply "




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
