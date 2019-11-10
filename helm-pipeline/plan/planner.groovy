def plan(String targetBranch, context, String kubernetesEnvironment) {

    utils = new io.polarpoint.utils.Utils()

    ansiColor('xterm') {


        def azureServicePrincipalCredentialId = context.config.azureServicePrincipalCredentialId
        def k8sVer = context.config.terraform.aks.k8sVer
        def aksNodes = context.config.terraform.aks.aksNodes
        def aksDiskSize = context.config.terraform.aks.aksDiskSize
        def aksVm = context.config.terraform.aks.aksVm
        def helmVer = context.config.terraform.aks.helmVer
        ansiColor('xterm') {



            try {

                    unstash 'init'

                withCredentials([azureServicePrincipal(azureServicePrincipalCredentialId)]) {
                    withEnv(["TF_VAR_client_secret=${AZURE_CLIENT_SECRET}","TF_VAR_client_id=${AZURE_CLIENT_ID}", "ARM_TENANT_ID=${AZURE_TENANT_ID}","ARM_SUBSCRIPTION_ID=${AZURE_SUBSCRIPTION_ID}","ARM_CLIENT_ID=${AZURE_CLIENT_ID}","ARM_CLIENT_SECRET=${AZURE_CLIENT_SECRET}"]) {

                        sh """
                             tree
                             terraform plan -var env=${kubernetesEnvironment} -var k8s_ver=${k8sVer} -var aks_nodes=${aksNodes} -var aks_disk_size=${aksDiskSize} -var aks_vm=${aksVm} -var helm_ver=${helmVer} -out plan.out
                         """
                    }
                }

                    stash name: 'plan',
                            includes: "plan.out"


            } catch (err) {
                error("plan failed: " + err.message)

            } finally {
                cleanWs notFailBuild: true
            }

        }
    }

}

def name() {
    return "scanner"
}

return this;
