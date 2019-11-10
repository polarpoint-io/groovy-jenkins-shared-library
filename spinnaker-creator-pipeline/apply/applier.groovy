def apply(String targetBranch, context) {


    def azureKeyVaultKubernetesClusters = context.config.azureKeyVaultKubernetesClusters
    def azureKeyVaultManagementClusters= context.config.azureKeyVaultManagementClusters
    def managementNameSpace= context.config.managementNameSpace
    def cliPod = context.config.spinnaker.cliPod
    def spinGateUrl = context.config.spinnaker.spinGateUrl
    def applicationList = context.config.applicationList
    def azureServicePrincipalCredentialId = context.config.azureServicePrincipalCredentialId
    def keyVaultURLOverride = context.config.keyVaultURL

            ansiColor('xterm') {

            unstash 'init'

            try {

                echo "Apply: about to run Spinnaker pipeline creator"
                echo "azureKeyVaultKubernetesClusters: ${azureKeyVaultKubernetesClusters} "

                def filename=azureKeyVaultManagementClusters+ '.config'

                // management cluster
                withAzureKeyvault(azureKeyVaultSecrets: [[secretType: 'Secret', name: azureKeyVaultManagementClusters, envVariable: 'storageKey']],credentialIDOverride: azureServicePrincipalCredentialId,keyVaultURLOverride: keyVaultURLOverride) {

                    echo "write $filename"
                    writeFile file: filename, text: storageKey

                }


                final allTemplates = findFiles(glob: 'templates/**/*')
                final allTemplateConfigs = findFiles(glob: 'template-configuration/**/*')


                def apps = readFile('applications.lst')

                def template = ""
                def templateConfig = ""

                for (String application : applicationList) {


                    def templateFileMatches = allTemplates.findAll { templates ->
                        allTemplates.any() { templates.name.contains(application) }
                    }

                    def templateConfigFileMatches = allTemplateConfigs.findAll { templateConfigs ->
                        allTemplateConfigs.any() { templateConfigs.name.contains(application) }
                    }


                    for (def templateFileConfig : allTemplateConfigs) {
                        if (templateFileConfig.name.contains(application)) {
                            templateConfig = templateFileConfig.name
                        }
                    }


                    for (def templateFileMatch : templateFileMatches) {
                        template = templateFileMatch.name

                        echo "Matching template: "+template.minus(".yml")

                        templateConfigMatch = templateConfigFileMatches.findAll{ configMatches ->
                            templateConfigFileMatches.any() { configMatches.name.contains(template.minus(".yml"))}
                            }

                        templateConfig= templateConfigMatch.name.join("")

                        echo "template file: ${template}"
                        echo "templateConfig file: ${templateConfig}"


                        if (template!="" && templateConfig!="") //ensure we have a template and config
                        {
                            echo "Update/Create Spinnaker Pipeline templates and config"

                            sh """    
                                    

                                     kubectl cp templates/$template $cliPod:/tmp --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace  --kubeconfig $filename
                                     kubectl cp template-configuration/$templateConfig $cliPod:/tmp --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace  --kubeconfig $filename
                                     kubectl exec $cliPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig $filename  -- bash -c  " export SPINNAKER_API=${
                                spinGateUrl
                            } ;  roer --verbose pipeline-template publish /tmp/$template "
                                    """
                            echo "Create Spinnaker Pipeline using template"
                            sh """
                
                    kubectl exec $cliPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig $filename  -- bash -c  "spin application save --application-name $application --owner-email surjit.bains@pouk01.onmicrosoft.com --cloud-providers 'kubernetes' --gate-endpoint ${
                                spinGateUrl
                            }  "
                    kubectl exec $cliPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig $filename  -- bash -c  " export SPINNAKER_API=${
                                spinGateUrl
                            } ;  roer  pipeline save  /tmp/$templateConfig "
                    kubectl exec $cliPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig $filename  -- bash -c  " export SPINNAKER_API=${
                                spinGateUrl
                            } ;  roer  pipeline-template publish /tmp/$template "
                    
                    """


                        } else {
                            echo "#############################################################"

                            echo "Warning: Missing template or template config for $application"

                            echo "#############################################################"
                        }


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
    return "applier"
}

return this;
