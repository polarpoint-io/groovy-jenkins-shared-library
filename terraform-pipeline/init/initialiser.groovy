def init(String targetBranch, context, String kubernetesEnvironment) {


    utils = new io.polarpoint.utils.Utils()

    ansiColor('xterm') {
        checkout scm

      //  try {
            def azureKeyVaultStorageKey = context.config.azureKeyVaultStorageKey
            def azureServicePrincipalCredentialId = context.config.azureServicePrincipalCredentialId
            def keyVaultURLOverride = context.config.keyVaultURL
            def terraformProject = context.config.terraform.url
            def gitHost = context.config.terraform.gitHost
            def credentialsID = context.config.terraform.credentialsId
            def sshCredentialsID = context.config.terraform.sshCredentialsId
            def branch = context.config.terraform.branch

            echo "Build: about to call terraform init  with "
            echo "azureKeyVaultStorageKey: ${azureKeyVaultStorageKey} "
            echo "kubernetesEnvironment: ${kubernetesEnvironment} "
            echo "terraformProject: ${terraformProject} "
            echo "credentialsID: ${credentialsID} "
            echo "sshCredentialsID: ${sshCredentialsID} "
            echo "branch: ${branch} "


            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "${credentialsID}", usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASS']]) {

                withAzureKeyvault(azureKeyVaultSecrets: [[secretType: 'Secret', name: azureKeyVaultStorageKey, envVariable: 'storageKey']],credentialIDOverride: azureServicePrincipalCredentialId,keyVaultURLOverride: keyVaultURLOverride) {

                        sshagent(credentials: [sshCredentialsID]) {
                        sh 'echo SSH_AUTH_SOCK=$SSH_AUTH_SOCK'
                        sh 'ls -al $SSH_AUTH_SOCK || true'
                        def statusCode = sh script:"export SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}; ssh-keygen -F ${gitHost}", returnStatus:true
                        if(statusCode != 0){
                            sh "mkdir -p ~/.ssh"
                            sh "export SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK}; ssh-keyscan ${gitHost} >> ~/.ssh/known_hosts"
                        }
                        git url: "https://${GITHUB_USER}:${GITHUB_PASS}@${terraformProject}", branch: "${branch}", credentialsId: "${credentialsID}"


                        sh """export SSH_AUTH_SOCK=${env.SSH_AUTH_SOCK};
                            terraform init -upgrade -reconfigure -backend-config access_key=${
                            storageKey
                        } -backend-config key=${kubernetesEnvironment} ;
                            """

                        stash name: 'init',
                                includes: "**/*"

                    }

                }
            }


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
    return "init"
}

return this;
