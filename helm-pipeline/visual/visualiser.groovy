def visual(String targetBranch, context) {

    utils = new io.polarpoint.utils.Utils()

    ansiColor('xterm') {


        def azureServicePrincipalCredentialId = context.config.azureServicePrincipalCredentialId
        def azureEnv = context.config.azureEnvironment
        ansiColor('xterm') {



            try {

                unstash 'init'

                withCredentials([azureServicePrincipal(azureServicePrincipalCredentialId)]) {
                    withEnv(["TF_VAR_client_secret=${AZURE_CLIENT_SECRET}","TF_VAR_client_id=${AZURE_CLIENT_ID}", "ARM_TENANT_ID=${AZURE_TENANT_ID}","ARM_SUBSCRIPTION_ID=${AZURE_SUBSCRIPTION_ID}","ARM_CLIENT_ID=${AZURE_CLIENT_ID}","ARM_CLIENT_SECRET=${AZURE_CLIENT_SECRET}"]) {

                        sh """
                             tree
                             terraform plan -var env=${azureEnv} -out plan.out
                         """
                    }
                }

                stash name: 'visual',
                        includes: "*"


            } catch (err) {
                error("plan failed: " + err.message)

            } finally {
                cleanWs notFailBuild: true
            }

        }
    }

}

def name() {
    return "init"
}

return this;
