def apply(context) {



    utils = new io.polarpoint.utils.Utils()

    def azureServicePrincipalCredentialId = context.config.azureServicePrincipalCredentialId
    def applyTerraform = context.config.applyTerraformWhiteList
        ansiColor('xterm') {

            unstash 'init'
            unstash 'plan'

            try {


                echo "Build: about to call terraform apply "
                withCredentials([azureServicePrincipal(azureServicePrincipalCredentialId)]) {
                    withEnv(["TF_VAR_client_secret=${AZURE_CLIENT_SECRET}", "TF_VAR_client_id=${AZURE_CLIENT_ID}", "ARM_TENANT_ID=${AZURE_TENANT_ID}", "ARM_SUBSCRIPTION_ID=${AZURE_SUBSCRIPTION_ID}", "ARM_CLIENT_ID=${AZURE_CLIENT_ID}", "ARM_CLIENT_SECRET=${AZURE_CLIENT_SECRET}"]) {
                        sh """
                          az login --service-principal --username ${AZURE_CLIENT_ID} --password ${
                            AZURE_CLIENT_SECRET
                        } --tenant ${AZURE_TENANT_ID}
                          mkdir -p ~/.helm/repository/
                          echo "apiVersion: v1
                          \ngenerated: 2006-01-02T15:04:05Z
                          \nrepositories: []" > ~/.helm/repository/repositories.yaml
                          cat ~/.helm/repository/repositories.yaml
                          ls -lrt ~/.helm/repository/repositories.yaml
                          
                           """

                        sh " terraform apply -auto-approve -lock=true plan.out"

                    }
                    }
                




            } catch (err) {
                error("apply failed: " + err.message)


            } finally {
                cleanWs notFailBuild: true
            }

        }

}

def name() {
    return "publisher"
}

return this;
