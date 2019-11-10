package io.polarpoint.git


@Grab(group = 'org.kohsuke', module = 'github-api', version = '1.92')
@GrabExclude('commons-lang:commons-lang')

import org.kohsuke.github.*

// store some things here so they are available everywhere
class Config {
    static Boolean doDelete = Boolean.valueOf(System.getProperty('branch.cleanup.doDelete', 'false'))
    static String  orgName = System.getProperty('branch.cleanup.gitOrgName', 'myOrg')
    static String  gitApiUrl = System.getProperty('branch.cleanup.gitApiUrl', 'https://git.myCompany.com/api/v3')
    static String  apiKey
}

def executeOnShell(String command, boolean log = false) {
    File workingDir = new File(System.properties.'user.dir')
    def process = new ProcessBuilder(addShellPrefix(command))
            .directory(workingDir)
            .redirectErrorStream(true)
            .start()
    if (log) {
        process.inputStream.eachLine { println it }
    }

    process.waitFor();
}

def addShellPrefix(String command) {
    def commandArray = new String[3]
    commandArray[0] = "sh"
    commandArray[1] = "-c"
    commandArray[2] = command
    return commandArray
}

def allRepos(GHOrganization org, String...repoPrefixes) {
    println "Fetching all repositories under the ${Config.orgName} org that match prefix(es) ${repoPrefixes}"
    return org.getRepositories().entrySet().findAll{ entry ->
        if (repoPrefixes) {
            return repoPrefixes.any{ prefix -> entry.key.startsWith(prefix) }
        } else {
            return true
        }
    }
}

def cleanupBranches(repo) {
    def defaultBranchName = repo.getDefaultBranch()
    def defaultBranch = repo.getBranch(defaultBranchName)
    def deletedBranchNames = []
    def branchesByName = repo.getBranches().entrySet().findAll{ !it.key.equals(defaultBranchName) && !it.value.isProtected() }.collectEntries{[it.key, it.value]}
    def pullRequests = repo.queryPullRequests().base(defaultBranchName).state(GHIssueState.CLOSED).list().withPageSize(100).each{ pr ->
        // loop thru all pull requests that have been closed and also merged
        if (pr.isMerged()) {
            def branch = branchesByName.get(pr.getHead().getRef())
            if (branch) {
                // the branch still exists and has been merged by this PR
                // make sure it doesn't have any unmerged commits
                def compare = repo.getCompare(defaultBranch, branch)
                if (compare.getTotalCommits() == 0) {
                    // branch has been merged and there are 0 commits since merge. delete it
                    println "Branch ${repo.getName()}/${branch.getName()} has 0 commits not merged to ${defaultBranchName}. Delete it. PR ${pr.getNumber()} : ${pr.getTitle()}"
                    if (Config.doDelete) {
                        deleteBranch(repo, branch)
                    }

                    // remove from internal map of branches since the branch has now been deleted in git
                    branchesByName.remove(branch.getName())
                    deletedBranchNames.push "${repo.getName()}/${branch.getName()}"
                }
            }
        }
    }
    return deletedBranchNames
}

def deleteBranch(repo, branch) {
    // use a simple curl here because the kohsuke library way of doing it requires 2 api calls when just 1 will do here
    String cmd = "curl -X DELETE -H \"Authorization: token ${Config.apiKey}\" ${Config.gitApiUrl}/repos/${Config.orgName}/${repo.getName()}/git/refs/heads/${branch.getName()}"
    executeOnShell(cmd)
}

if (args.size() < 1) {
    println "Usage: cleanupRepoBranches.groovy <oauthToken> <optionalRepo-name>"
    System.exit(1)
}

Config.apiKey = args[0]

def branchesDeleted = []
def errors = []
GitHub github = GitHub.connectToEnterprise(Config.gitApiUrl, Config.apiKey)
if (args.size() > 1) {
    String repoName = args[1]
    GHRepository repo = github.getRepository("${Config.orgName}/${repoName}")
    branchesDeleted = cleanupBranches(repo)
} else {
    def repoPrefixes = System.getProperty('branch.cleanup.repoPrefixes', 'pref-,pref2-').split(',')
    def answer = System.console().readLine "You have not specified a repoName. If you proceed, this script will list ${Config.doDelete ? 'and delete ' : ''}all branches with a merged pull request and 0 commits left to merge for all repos starting with ${repoPrefixes.join(', ')} in the ${Config.orgName} org. Are you sure? (y/n) "
    if (answer == 'y') {
        println 'ok! here we go!'
        allRepos(github.getOrganization(Config.orgName), repoPrefixes).each { entry ->
            try {
                branchesDeleted += cleanupBranches(entry.value)
            } catch (Exception e) {
                errors.push([ message: "Error processing branches for ${entry.key} repo", ex: e ])
            }
        }
    }
}
println "${branchesDeleted.size()} Branches deleted..."
branchesDeleted.each{ branch -> println branch }
println "${errors.size()} errors..."
errors.each{ error ->
    println error.message
    error.ex.printStackTrace()
    println
}