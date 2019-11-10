import io.polarpoint.git.BranchFinder
import io.polarpoint.git.RepoFinder
import io.polarpoint.git.TableFormatter

def call(Integer daysOld, String org) {
    def getMoreRefs = 'git for-each-ref refs/remotes  --format=\"%(objectname:short),%(authorname),%(authoremail),%(refname:short),%(committerdate:short)\" 2>&1'
    def repos = new RepoFinder().findAllRepos(org)

    println 'Found ' + repos.size() + ' repos!'
    ansiColor('xterm') {
        for (repo in repos) {


            println 'Repo - ' + repo


            def allRefs = []
            def refsToDelete = []
            def mailTo = ''
            def body = ''
            def recipients = []

            node('master') {

                cleanWs notFailBuild: true

                try {
                    git credentialsId: '43cee12e-c6be-41e5-b5c3-79e3c47c1292', url: repo
                    allRefs = sh(script: getMoreRefs, returnStdout: true).split('\n')
                    refsToDelete = new BranchFinder().findStaleBranches(allRefs, daysOld);
                } catch (err) {
                    println 'Error with ${repo}' + err.message

                }



                if (refsToDelete.size() > 0) {

                    for (line in refsToDelete) {
                        recipients << line[2].replaceAll('(<|>)', '')
                    }

                    recipients = ['neil@winder-it.co.uk']
                    mailTo = recipients.unique().join(" ")
                    body = new TableFormatter(refs: refsToDelete, repo: repo).makeHtmlTable()

                    def subject = "Testing! Stale branches in ${repo} old than ${daysOld} days scheduled for deletion!"

                    try {
                        mail mimeType: 'text/html', body: body, subject: subject, to: mailTo
                    }
                    catch (err) {
                        println err.message
                        println "Failed to send email for ${repo}!"
                        println 'Body' + body
                        println 'Mail To' + mailTo
                    }

                    for (line in refsToDelete) {
                        r = repo.replaceAll('https://', '')
                        println "Deleting: ${r}/${line[3]}"
                        try {
                            withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: '43cee12e-c6be-41e5-b5c3-79e3c47c1292', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASS']]) {
                                sh "git push https://${GITHUB_USER}:${GITHUB_PASS}@${r} --delete ${line[3]}"
                            }
                        } catch (err) {
                            println err.message
                        }
                    }

                } else {
                    println "No branches older than ${daysOld} found!"
                }
            }

        }
    }
}


