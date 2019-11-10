def apply(String targetBranch, context) {


    def azureKeyVaultKubernetesClusters = context.config.azureKeyVaultKubernetesClusters
    def azureKeyVaultManagementClusters= context.config.azureKeyVaultManagementClusters
    def managementNameSpace= context.config.managementNameSpace
    def halyardPod = context.config.spinnaker.halyardPod

            ansiColor('xterm') {

            unstash 'init'

            try {

                echo "Build: about to apply "
                def filename=azureKeyVaultManagementClusters+ '.config'
                sh """
                        kubectl  get pods --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace  --kubeconfig $filename
                        kubectl exec $halyardPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig $filename -i  kubectl config get-contexts  
                        kubectl exec $halyardPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig $filename -i  bash hal backup create > backup.out
                        kubectl exec $halyardPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig $filename -i  bash hal config provider kubernetes account list > account.lst
                   """
                // get a list of spinnaker kubernetes provider accounts
                // to see if we need to add or edit the account

                def accountList = readFile 'account.lst'

                for (String azureKeyVaultKubernetesCluster : azureKeyVaultKubernetesClusters) {
                    def kubeConfig = azureKeyVaultKubernetesCluster+ '.config'
                    def addOrEdit = accountList.contains(azureKeyVaultKubernetesCluster)
                    def halAdd = "hal config provider kubernetes account add $azureKeyVaultKubernetesCluster  --docker-registries nexus-registry --context $azureKeyVaultKubernetesCluster  --provider-version v2 --kubeconfig-file /home/spinnaker/.kube/$kubeConfig"
                    def halEdit = "hal config provider kubernetes account edit $azureKeyVaultKubernetesCluster  --docker-registries nexus-registry --context $azureKeyVaultKubernetesCluster  --provider-version v2 --kubeconfig-file /home/spinnaker/.kube/$kubeConfig"



                    sh " kubectl cp $kubeConfig $halyardPod:/home/spinnaker/.kube/ --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace  --kubeconfig $filename"
                    if (addOrEdit) // if we find an existing provider account edit otherwise add
                    {
                        sh """
                              kubectl exec $halyardPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig $filename -i  -- bash -c "$halEdit"
                           """
                      }
                    else
                    {
                        sh """
                              kubectl exec $halyardPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig $filename -i  -- bash -c "$halAdd"
                           """

                    }
                }

                sh """
                              kubectl exec $halyardPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig $filename -i  bash hal deploy apply
                   """

                sh """
                            kubectl exec $halyardPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig $filename -i  kubectl config get-contexts 
                   """

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
