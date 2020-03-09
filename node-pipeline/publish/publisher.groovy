def publish(String targetBranch, context) {
    // because the version has updated
    if (targetBranch =~ /development/) {
        unstash 'package.json'
    }

    try {
        sh 'cp Staticfile build/'
        withNPM(npmrcConfig: 'npmrc-new') {
            echo "Performing npm publish"
            sh 'npm publish'
        }

    } catch (err) {
        echo(err.message)
        error "Failed to publish artifact to Nexus"
    } finally {
        cleanWs()
    }
}

def name() {
    return "publisher"
}


return this;
