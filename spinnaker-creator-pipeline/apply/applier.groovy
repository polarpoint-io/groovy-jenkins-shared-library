def apply(String targetBranch, context) {


    def azureKeyVaultKubernetesClusters = context.config.azureKeyVaultKubernetesClusters
    def azureKeyVaultManagementClusters= context.config.azureKeyVaultManagementClusters
    def managementNameSpace= context.config.managementNameSpace
    def cliPod = context.config.spinnaker.cliPod
    def spinGateUrl = context.config.spinnaker.spinGateUrl
    def applicationList = context.config.applicationList
    def azureServicePrincipalCredentialId = context.config.azureServicePrincipalCredentialId
    def keyVaultURLOverride = context.config.keyVaultURL

    def globalTemplatesRepoCredentialsId = context.config.globalTemplates?.repoCredentialsId?: false
    def globalTemplatesRepoURL = context.config.globalTemplates?.repoURL?: false
    def globalTemplatesRepoBranch = context.config.globalTemplates?.repoBranch?: false

    def debugSpinnakerKubectlCommands = context.config.debugSpinnakerKubectlCommands?: false

    def isGlobalTemplateConfigComplete = globalTemplatesRepoCredentialsId && globalTemplatesRepoURL && globalTemplatesRepoBranch

            ansiColor('xterm') {

            unstash 'init'

            try {
                if(debugSpinnakerKubectlCommands) {
                    echo "*** Debug Spinnaker Kubectl Commands is ENABLED. Commands won't be actually executed."
                }

                echo "Apply: about to run Spinnaker pipeline creator"
                echo "azureKeyVaultKubernetesClusters: ${azureKeyVaultKubernetesClusters} "

                def filename=azureKeyVaultManagementClusters+ '.config'
                def template = ""
                def templateConfig = ""
                def command = ""

                // management cluster
                withAzureKeyvault(azureKeyVaultSecrets: [[secretType: 'Secret', name: azureKeyVaultManagementClusters, envVariable: 'storageKey']],credentialIDOverride: azureServicePrincipalCredentialId,keyVaultURLOverride: keyVaultURLOverride) {

                    echo "write $filename"
                    writeFile file: filename, text: storageKey

                }

                /*
                    Save global-common templates
                 */
                echo "Global Template Config:"
                echo "======================="
                echo "Repo Credentials Id: ${globalTemplatesRepoCredentialsId}"
                echo "Repo URL: ${globalTemplatesRepoURL}"
                echo "Repo Branch: ${globalTemplatesRepoBranch}"
                echo "======================="

                if(isGlobalTemplateConfigComplete) {

                    echo "Found Global Templates configuration. Loading Global Templates..."

                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: globalTemplatesRepoCredentialsId, usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASS']]) {
                        dir('globalTemplates') {
                            git url: "https://${GITHUB_USER}:${GITHUB_PASS}@${globalTemplatesRepoURL}", branch: "${globalTemplatesRepoBranch}", credentialsId: globalTemplatesRepoCredentialsId

                            sh "ls -al"

                            final allGlobalTemplates = findFiles(glob: 'templates/**/*')

                            echo "All Global Templates: ${allGlobalTemplates}"

                            def globalTemplateFileMatches = allGlobalTemplates

                            echo "Global Templates File Matches: ${globalTemplateFileMatches}"

                            def isExistingGlobalTemplates = globalTemplateFileMatches.size() > 0

                            for (def templateFileMatch : globalTemplateFileMatches) {
                                template = templateFileMatch.name

                                echo "Matching global template: "+template.minus(".yml")
                                echo "template file: ${template}"

                                echo "Update/Create Spinnaker Pipeline template"

                                command = """    
                                  
                                     kubectl cp templates/$template $cliPod:/tmp --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace  --kubeconfig ../$filename
                                     kubectl exec $cliPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig ../$filename  -- bash -c  " export SPINNAKER_API=${
                                    spinGateUrl
                                } ;  roer --verbose pipeline-template publish /tmp/$template "
                                    """
                                echo "Executing command: '${command}'"

                                if(!debugSpinnakerKubectlCommands) sh command
                            }

                        }
                    }

                }
                else {
                    echo "Global Templates configuration is incomplete. Not loading global templates."
                }
                // **********************************************************************

                def apps = readFile('applications.lst')

                final allTemplates = findFiles(glob: 'templates/**/*')
                final allTemplateConfigs = findFiles(glob: 'template-configuration/**/*')

                echo "Found templates: ${allTemplates}"
                echo "Found configs: ${allTemplateConfigs}"

                /*
                    Save project-common templates
                 */
                def commonTemplateNameToken = "common"
                def commonTemplateFileMatches = allTemplates.findAll { templates ->
                    allTemplates.any() { templates.name.contains(commonTemplateNameToken) } }

                def isExistingCommonTemplates = commonTemplateFileMatches.size() > 0

                for (def templateFileMatch : commonTemplateFileMatches) {
                    template = templateFileMatch.name

                    echo "Matching common template: "+template.minus(".yml")
                    echo "template file: ${template}"

                    echo "Update/Create Spinnaker Pipeline template"

                    command = """    
                                  
                                     kubectl cp templates/$template $cliPod:/tmp --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace  --kubeconfig $filename
                                     kubectl exec $cliPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig $filename  -- bash -c  " export SPINNAKER_API=${
                        spinGateUrl
                    } ;  roer --verbose pipeline-template publish /tmp/$template "
                                    """

                    echo "Executing command: '${command}'"

                    if(!debugSpinnakerKubectlCommands) sh command

                }

                // ***

                echo "Iterating over applicationList: " + applicationList
                for (String application : applicationList) {

                    echo "*********************************************** "
                    echo "* Application: " + application
                    echo "*********************************************** "

                    def templateFileMatches = allTemplates.findAll { templates ->
                        allTemplates.any() { templates.name.contains(application) }
                    }

                    def templateConfigFileMatches = allTemplateConfigs.findAll { templateConfigs ->
                        allTemplateConfigs.any() { templateConfigs.name.contains(application) }
                    }

                    echo "No of application templates found: ${templateFileMatches.size()}"
                    echo "No of application configurations found: ${templateConfigFileMatches.size()}"



                    // Publish all application templates if any
                    for (def templateFileMatch : templateFileMatches) {
                        template = templateFileMatch.name

                        echo "Matching template: " + template.minus(".yml")
                        echo "template file: ${template}"

                        echo "Update/Create Spinnaker Pipeline template"

                        command = """    
                            

                             kubectl cp templates/$template $cliPod:/tmp --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace  --kubeconfig $filename
                             kubectl cp template-configuration/$templateConfig $cliPod:/tmp --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace  --kubeconfig $filename
                             kubectl exec $cliPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig $filename  -- bash -c  " export SPINNAKER_API=${
                            spinGateUrl
                        } ;  roer --verbose pipeline-template publish /tmp/$template "
                            """

                        echo "Executing command: '${command}'"

                        if(!debugSpinnakerKubectlCommands) sh command

                    }

                    echo "*********************************************** "

                    // Save all application config if any
                    if(templateConfigFileMatches.size() > 0) {
                        echo "Update/Create application ${application}"

                        command = """
        
                            kubectl exec $cliPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig $filename  -- bash -c  "spin application save --application-name $application --owner-email surjit.bains@pouk01.onmicrosoft.com --cloud-providers 'kubernetes' --gate-endpoint ${
                            spinGateUrl
                        }  "                            
                            """

                        echo "Executing command: '${command}'"

                        if(!debugSpinnakerKubectlCommands) sh command
                    }


                    for (def templateConfigFileMatch : templateConfigFileMatches) {
                        configFile = templateConfigFileMatch.name

                        echo "Matching config: " + configFile.minus(".yml")
                        echo "config file: ${configFile}"

                        echo "Update/Save Spinnaker Pipeline configuration"

                        command = """
                            kubectl cp template-configuration/$configFile $cliPod:/tmp --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace  --kubeconfig $filename
                            kubectl exec $cliPod --context=$azureKeyVaultManagementClusters --namespace=$managementNameSpace --kubeconfig $filename  -- bash -c  " export SPINNAKER_API=${
                            spinGateUrl
                        } ;  roer  pipeline save  /tmp/$configFile "
                                                        
                            """

                        echo "Executing command: '${command}'"

                        if(!debugSpinnakerKubectlCommands) sh command

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
