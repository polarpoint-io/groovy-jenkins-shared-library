import io.polarpoint.workflow.Utils

def init(String targetBranch, context) {



    ansiColor('xterm') {
        checkout scm

          try {
        def azureKeyVaultKubernetesClusters = context.config.azureKeyVaultKubernetesClusters
        def azureKeyVaultManagementClusters= context.config.azureKeyVaultManagementClusters
        def azureServicePrincipalCredentialId = context.config.azureServicePrincipalCredentialId
        def keyVaultURLOverride = context.config.keyVaultURL

        echo "Build: about to sync :  ${azureServicePrincipalCredentialId}"
        echo "azureKeyVaultKubernetesClusters: ${azureKeyVaultKubernetesClusters} "
        echo "keyVaultURLOverride: ${keyVaultURLOverride}"



        // management cluster
        withAzureKeyvault(azureKeyVaultSecrets: [[secretType: 'Secret', name: azureKeyVaultManagementClusters, envVariable: 'storageKey']],credentialIDOverride: azureServicePrincipalCredentialId,keyVaultURLOverride: keyVaultURLOverride) {
            def filename=azureKeyVaultManagementClusters+ '.config'
            echo "write $filename"
            writeFile file: filename, text: storageKey

        }


        // all other clusters
        def kubeconfig=""
        for (String azureKeyVaultKubernetesCluster : azureKeyVaultKubernetesClusters) {


            withAzureKeyvault(azureKeyVaultSecrets: [[secretType: 'Secret', name: azureKeyVaultKubernetesCluster, envVariable: 'storageKey']],credentialIDOverride: azureServicePrincipalCredentialId,keyVaultURLOverride: keyVaultURLOverride) {
                def filename=azureKeyVaultKubernetesCluster + '.config'
                echo "write $filename"
                writeFile file: filename, text: storageKey
                def currentConfig= filename
                kubeconfig=kubeconfig.concat(currentConfig+":")
            }
        }

        sh  """
                export KUBECONFIG=$kubeconfig
                kubectl config view --flatten > all.config

            """


        stash name: 'init',
                includes: "**/*"

        } catch (err) {
            error("init failed: " + err.message)
            err.printStackTrace()
              Utils.stackTrace(err)


        }
          //finally {
//            cleanWs notFailBuild: true
//        }

    }

}

def name() {
    return "initialiser"
}

return this;