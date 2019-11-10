def init(String targetBranch, context) {

    utils = new io.polarpoint.utils.Utils()

    ansiColor('xterm') {
        checkout scm

        //  try {
        def azureKeyVaultKubernetesClusters = context.config.azureKeyVaultKubernetesClusters
        def azureKeyVaultManagementClusters= context.config.azureKeyVaultManagementClusters
        def managementNameSpace= context.config.managementNameSpace
        def cliPod = context.config.spinnaker.cliPod
        def spinGateUrl = context.config.spinnaker.spinGateUrl
        def applicationList = context.config.applicationList
        def azureServicePrincipalCredentialId = context.config.azureServicePrincipalCredentialId
        def keyVaultURLOverride = context.config.keyVaultURL

        echo "Build: about to run Spinnaker pipeline creator"
        echo "azureKeyVaultKubernetesClusters: ${azureKeyVaultKubernetesClusters} "

        def filename=azureKeyVaultManagementClusters+ '.config'

        // management cluster
        withAzureKeyvault(azureKeyVaultSecrets: [[secretType: 'Secret', name: azureKeyVaultManagementClusters, envVariable: 'storageKey']],credentialIDOverride: azureServicePrincipalCredentialId,keyVaultURLOverride: keyVaultURLOverride) {

            echo "write $filename"
            writeFile file: filename, text: storageKey

        }
        sh """
          
            kubectl exec $cliPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig $filename  -- bash -c "curl $spinGateUrl/applications | jq -r '..|.name? | select(.. != null)'" > applications.lst
        """

        def apps = readFile('applications.lst')


        echo "currentApplications"
        echo "$apps"


        stash name: 'init',
                includes: "**/*"



//        } catch (err) {
//            error("init failed: " + err.message)
//            err.printStackTrace()
//
//
//        } finally {
//            cleanWs notFailBuild: true
//        }

    }

}

def name() {
    return "initialiser"
}


return this;
