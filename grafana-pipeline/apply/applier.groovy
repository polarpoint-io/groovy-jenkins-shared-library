 def apply(String targetBranch, context) {


     utils = new io.polarpoint.utils.Utils()

     ansiColor('xterm') {
         try {

             utils.printColour("OWASP: about to update OWASP NVD", 'green')
             checkout scm


             //dependencyCheck additionalArguments: '''--format XML --data /mnt/nvd/ --updateonly --log update.log''', odcInstallation: '5.2.4'
             sh './exporter.sh'


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
