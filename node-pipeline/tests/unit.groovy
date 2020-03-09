def runTest(String targetBranch, context) {
    ansiColor('xterm') {
//        container('nodejs') {
            checkout scm
            try {

                withEnv([
                        /* Override the npm cache directory to avoid: EACCES: permission denied, mkdir '/.npm' */
                        'npm_config_cache=npm-cache',
                        /* set home to our current directory because other bower
                        * nonsense breaks with HOME=/, e.g.:
                        * EACCES: permission denied, mkdir '/.config'
                        */
                        'HOME=.',
                ]) {
                    sh """
				        npm install --no-package-lock
                        npm run build
				    """
                    stash name: 'node-artifacts', includes: 'build/**/*,Dockerfile'
                    stash name: 'Dockerfile', includes: 'Dockerfile'
                    sh "CI=true npm run test"
                }
            } catch (err) {
                echo 'Unit test failed!'
                error(err.message)
            } finally {

            }
        }
//    }
}


return this
