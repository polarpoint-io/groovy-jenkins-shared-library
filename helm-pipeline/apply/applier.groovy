def apply(context) {



    utils = new io.polarpoint.utils.Utils()


    def applicationName= context.config.applicationName
    def helmRepo = context.config.helmRepo

        ansiColor('xterm') {

            unstash 'init'


            sh """
                    
                    helm repo index .
               """




        }

}

def name() {
    return "publisher"
}

return this;
