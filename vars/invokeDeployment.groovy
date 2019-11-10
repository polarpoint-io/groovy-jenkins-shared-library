/*
 * Surj Bains  <surj@polarpoint.io>
 * InvokePipeline
 * Entry point for all Jobs
 * Called from the Jenkinsfile
 */

@Grab(group = 'org.pegdown', module = 'pegdown', version = '1.4.1')
@Grab(group ='net.steppschuh.markdowngenerator',module = 'markdowngenerator',version = '1.3.0.0')

import org.pegdown.PegDownProcessor
import net.steppschuh.markdowngenerator.table.Table
import groovy.text.SimpleTemplateEngine

import groovy.json.StringEscapeUtils
import groovy.text.SimpleTemplateEngine
import io.polarpoint.workflow.ConfigurationContext
import groovy.json.JsonBuilder


echo("[Deployment] invoked")

def call(String application, String configuration, String deployables, String cf_space) {
    ConfigurationContext configurationContext
    node('master') {
        def REPO = scm.userRemoteConfigs.getAt(0).getUrl()
        REPO = REPO - 'https://'

        String currentBranch = env.BRANCH_NAME

        //ensure we take a deep clone
        checkout([
                $class           : 'GitSCM',
                branches         : [[name: currentBranch]],
                extensions       : [[$class: 'CloneOption', depth: 0, noTags: false, reference: '', shallow: false], [$class: 'LocalBranch', localBranch: '']],
                userRemoteConfigs: scm.userRemoteConfigs,
                repository       : scm.repositories,

        ])

        sh 'git config --global push.default simple'

        configurationContext = new ConfigurationContext(application, readFile(configuration),env.WORKSPACE)
        configurationContext = prepareDeployment(configurationContext,deployables,cf_space, REPO)
        def deploymentNotes = new File(env.WORKSPACE + "/pipelines/templates/deploymentNotes.md")

        stash name: 'pipelines', includes: 'pipelines/**'
        echo("[Pipeline] calling  with application:" + application)
        echo("[Pipeline] calling  with Branch:  $env.BRANCH_NAME ")
        echo("[Pipeline] calling  with configuration:" + ( new JsonBuilder( configurationContext ).toPrettyString()))
        echo("[Pipeline] deploymentNotes")

        echo (deploymentNotes.text)

        deploymentWorkflow(configurationContext, env.BRANCH_NAME, deploymentNotes.text)
        echo("[Pipeline] End.")
    }
}


def ConfigurationContext prepareDeployment (ConfigurationContext  context, String deployables , String cf_space, String REPO)
{
    def cloudSpace = context.config.cloudFoundry.cloudSpace
    def springCloudEnv = context.config.cloudFoundry.springEnv

    if (deployables!="") { // either use the parameters from the job or the ones supplied in the configuration.json
        def deployJson = StringEscapeUtils.unescapeJavaScript(deployables.substring(1, deployables.length() - 1))
        def listDeployables =
                new groovy.json.JsonSlurperClassic().
                        parseText(deployJson)

        context.config.cloudFoundry.cloudSpace = cf_space
        context.config.cloudFoundry.springEnv = cf_space.tokenize('-').last()

        def hashDeployables = listDeployables.collectEntries {
            b -> [b.application, b.version]
        }

        echo " hashDeployables: " + (new JsonBuilder(hashDeployables).toPrettyString())

        def templateDeployables = context.config.manifest.artifacts
        //compare dashboard json against the configuration.json in pipelines/conf/
        templateDeployables.each { deploy ->

            def hash = hashDeployables.get(deploy.key)

            if (hash != null) {
                // update the version to the one selected
                templateDeployables.get(deploy.key).version = hash
            }

        }
        echo("updated templateDeployables: " + (new JsonBuilder(templateDeployables).toPrettyString()))
        context.config.manifest.artifacts = templateDeployables

        def jsonBuilder = new groovy.json.JsonBuilder(context)
        json = context.asJsonString()
        jsonBuilder = null
        CURRENT_PATH = sh(script: 'pwd', returnStdout: true).trim()
        def configurationJsonFile = new File(CURRENT_PATH + "/pipelines/conf/configuration.json")
        configurationJsonFile.write(json)
    }
    CURRENT_PATH = sh(script: 'pwd', returnStdout: true).trim()


    try {
        // git tag the version
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: '43cee12e-c6be-41e5-b5c3-79e3c47c1292', usernameVariable: 'GITHUB_USER', passwordVariable: 'GITHUB_PASS']]) {
            deploymentNotes( context, CURRENT_PATH + "/pipelines/templates/")
            publishHTML (target: [
                    allowMissing: false,
                    alwaysLinkToLastBuild: false,
                    keepAll: true,
                    reportDir: 'pipelines/templates/',
                    reportFiles: 'deploymentNotes.html',
                    reportName: "Deployment Notes"
            ])
            config = sh(returnStdout: true, script: "git add pipelines/*")
            configCommit = sh(returnStdout: true, script: "git commit -m '[jenkins-versioned]' ")
            tagPush = sh(returnStdout: true, script: "git push https://${GITHUB_USER}:${GITHUB_PASS}@${REPO}  && git push https://${GITHUB_USER}:${GITHUB_PASS}@${REPO}  ")

        }
    } catch (err) {
        error('Unable to log changes for deployment ' + err.message)


    }


    return context

}



def deploymentNotes(ConfigurationContext context, String templatePath) {
    def deploymentNotes = new File(templatePath+'deploymentNotes.md')
    deploymentNotes.write("")
    def buildUser
    wrap([$class: 'BuildUser']) {
       buildUser = "${BUILD_USER}"
    }
    def cloudSpace = context.config.cloudFoundry.cloudSpace
    def artifacts = context.config.manifest.artifacts
    def deploymentTitle="Deployment to :"+cloudSpace +" "+new Date().format("HH:mm:ss dd-MMM-yyy") + " by :"+ buildUser
    def application = context.config.application

    deploymentNotes << "# ${deploymentTitle}\n"


    Table.Builder tableBuilder = new Table.Builder()
            .withAlignments(Table.ALIGN_RIGHT, Table.ALIGN_LEFT)
            .addRow("component", "group", "version")

    artifacts.each { key, value ->
        tableBuilder.addRow(value.artifactId,value.groupId,value.version)
    }

    deploymentNotes << tableBuilder.build()



    def pdp = new PegDownProcessor()
    def engine = new SimpleTemplateEngine()
    println 'Using template file at :'+templatePath
    def template = engine.createTemplate(new File(templatePath +"deploymentNotes.tpl"))
    def markdown = [deploymentNotes: pdp.markdownToHtml(new File(templatePath+"deploymentNotes.md").text), deploymentTitle: deploymentTitle, application: application, deploymentTitle: pdp.markdownToHtml(deploymentTitle)]
    def result = template.make(markdown)
    def deploymentNotesHTML = new File(templatePath+'deploymentNotes.html')
    deploymentNotesHTML.write(result.toString())

}


return this
