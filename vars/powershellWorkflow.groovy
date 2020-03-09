import io.polarpoint.workflow.contexts.PowershellContext

def call(PowershellContext context, String targetBranch, scmVars, Boolean toTag) {
    def stageHandlers = context.getConfigurablePowershellHandlers()
    def applier
    def success = false
    def label = "powershell"

    node('master') {

        echo("target_branch:" + targetBranch)
        checkout scm

        echo("Loading Applier: " + stageHandlers.getApply())
        applier = load(stageHandlers.getApply())

    }

    try
    {
        podTemplate(label: label)
        {
            node('powershell')
            {
                container('powershell')
                {
                    stage("apply")
                    {
                        applier.apply(targetBranch, context)

                        // If required add the data to GitHub
                        if (context.config.scriptLocation.url.contains("metrics-reports.git")) {
                            unstash 'metrics-reports'
                            def REPO = scm.userRemoteConfigs.getAt(0).getUrl()
                            REPO = REPO - 'https://'                            
                            
                            // add the files to the repo
                            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: '43cee12e-c6be-41e5-b5c3-79e3c47c1292', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASS']]) {                                                                                   
                                reportData = sh(returnStdout: true, script: "git add ${env.WORKSPACE}/Reports/Data/by*.json")                                                                                                
                                sh 'git config --global user.email \"jenkins@mycnets.com\"'
                                sh 'git config --global user.name \"Jenkins Server\"'                
                                configCommit = sh(returnStdout: true, script: "git commit -m '[jenkins-versioned] added metric report and data.'")                
                                tagPush = sh(returnStdout: true, script: "git push https://${GITHUB_USER}:${GITHUB_PASS}@${REPO}")
                            }
                        }
                    }
                }
            }
        }        

        success = true
    }

    catch (err) {

        echo err.message
        error("Powershell tasks have failed.")

    } finally {
        // Notify----------------------------//
        stage("Notify") {
            try {
                if (success) {
                    slackSend(color: '#00FF00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
                } else {
                    slackSend(color: '#FF0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})")
                    echo "Pipeline tasks have failed."
                }

            } catch (err) {
                println err.message
                error "Notifications failed."
            } finally {

                // do nothing
            }
        }
    }
}

return this;
