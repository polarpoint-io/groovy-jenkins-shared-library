def call(scmVars, description, context, _state) {

    def credsId = '2cebd8e6-66a9-473c-bacd-b63adb436e1d'
    def u = new io.polarpoint.utils.Utils()

    def state = _state ?: 'ERROR'


    if (u.getCredentialsById(credsId)) {
        try {
            def splitRepo = scmVars.GIT_URL.split('/')

            // pol_hngt_basketservice
            def repo = splitRepo[4].replaceFirst(/\.git/, "")
            // pol
            def account = splitRepo[3]

            githubNotify account: account,
                    context: context,
                    credentialsId: credsId,
                    description: description,
                    gitApiUrl: 'https://github.pohzn.com/api/v3',
                    repo: repo,
                    sha: scmVars.GIT_COMMIT,
                    status: state

        } catch (err) {
            println """
            state: ${state}  
            context: ${context}
            description ${description}
            sha: ${scmVars.GIT_COMMIT}
            """
            println err.message
            u.printColour('Github Notify Failed!', 'red')
        }
    }

}