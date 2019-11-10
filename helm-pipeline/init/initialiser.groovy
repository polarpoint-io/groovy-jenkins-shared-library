def init(String targetBranch, context, String kubernetesEnvironment) {


    utils = new io.polarpoint.utils.Utils()

    ansiColor('xterm') {
        checkout scm


            def applicationName= context.config.applicationName


        sh """
            

            """
        stash name: 'init',
                includes: "**/*.tgz"

    }

}

def name() {
    return "init"
}

return this;
