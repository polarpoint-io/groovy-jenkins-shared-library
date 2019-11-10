package io.polarpoint.git

import com.cloudbees.groovy.cps.NonCPS

@Grab(group = 'org.kohsuke', module = 'github-api', version = '1.92')
@GrabExclude('commons-lang:commons-lang')
import org.kohsuke.github.*

class RepoFinder {



    @NonCPS
    def findAllRepos(String org) {

        def token = 'c50662cded6f3b5e034910bd0fa2cae0519ec24d'
        def gitApiUrl = 'https://github.pohzn.com/api/v3'
        def foundRepos = []

        GitHub github = GitHub.connectToEnterprise(
                gitApiUrl,
                token);

        try {
            github.checkApiUrlValidity()

        } catch (HttpException e) {
            println('Problem connecting to github')
        }

        def repos =  github.getOrganization(org).listRepositories()

        foundRepos = repos.collect { r ->
            r.gitHttpTransportUrl()
        }

        return foundRepos
    }
}

