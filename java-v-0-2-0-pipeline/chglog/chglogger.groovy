def createlog(String targetBranch, context) {

    utils = new io.polarpoint.utils.Utils()    

    try {
        echo "Chglog: about to create Change Log with branch ${targetBranch} ."
        
        milestone(label: 'Chglog')
        podTemplate(label: 'gitchglog') {
            node('gitchglog') {                  
                container('gitchglog') {
                    def REPO = scm.userRemoteConfigs.getAt(0).getUrl()
                    REPO = REPO - 'https://'

                    String currentBranch = targetBranch

                    //ensure we take a deep clone to get the tags (if any)
                    checkout([
                            $class           : 'GitSCM',
                            branches         : [[name: currentBranch]],
                            extensions       : [[$class: 'CloneOption', depth: 0, noTags: false, reference: '', shallow: false], [$class: 'LocalBranch', localBranch: '']],
                            userRemoteConfigs: scm.userRemoteConfigs,
                            repository       : scm.repositories,

                    ])

                    sh "git-chglog --output ${env.WORKSPACE}/CHANGELOG.md --config ${env.WORKSPACE}/pipelines/templates/config.yml"
                    
                    stash name: 'chglog',
                        includes: "**/CHANGELOG.md"
                }
            }
        } 

    } catch (err) {
        error("Chglog creation failed: " + err.message)
    }

}

def name() {
    return "createlog"
}

return this;
