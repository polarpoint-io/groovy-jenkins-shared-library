def report(String targetBranch, context) {


    ansiColor('xterm') {
        unstash 'apply'

        publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true,
                     reportDir: '', reportFiles: 'index.html',
                     reportName: 'Git Repository Checker', reportTitles: ''])
    }

}

def name() {
    return "reporter"
}

return this;
