
package io.polarpoint.utils

import groovy.json.*
import io.polarpoint.utils.Utils
import java.io.Serializable
import java.io.File;



class Slack implements Serializable {
    private def pipeline
    private def exceptionInBuild
    private boolean hasAnsiSupport
    private boolean disableAnsi;

    Slack(pipeline, disableAnsi = false) {
        this.pipeline = pipeline
        this.disableAnsi = disableAnsi
        try {
            Class.forName('hudson.plugins.ansicolor.AnsiColorBuildWrapper', false, pipeline.getClass().getClassLoader())
            this.hasAnsiSupport = true
        } catch (java.lang.ClassNotFoundException e) {
            this.hasAnsiSupport = false
        }
    }

    def void sender(config) {
        this.sender(true, config)
    }

    /**
     * get the change log aggregated message in newline separated string
     * @return aggregated change log
     */
    @NonCPS
    def String getChangeLogMessage() {
        def changeLogSets = pipeline.currentBuild.changeSets
        def changeLogMessage = "${pipeline.currentBuild.changeSets.size()} Repository change(s)\n"
        def totalCommits = 0
        def fileChanges = 0
        def author =""
        def commitMsg =""
        def commitDetail=""
        def commitTimeStamp=""
        def commitId=""
        pipeline
                .currentBuild
                .changeSets
                .each { changeLogSet ->
                    totalCommits += changeLogSet.items.size()
                    changeLogSet.items.each { changeSet ->

                        author = changeSet.getAuthor()
                        commitMsg = changeSet.getMsg()
                        fileChanges += changeSet.affectedFiles.size()
                        commitTimeStamp = new Date (changeSet.getTimestamp()).toString()
                        commitId = changeSet.getCommitId()
                        commitDetail += "*Commit* ${commitMsg} *By* ${author}  \n"
                        commitDetail += "*CommitId* ${commitId} *On* ${commitTimeStamp}  \n"
                        if (changeSet.affectedFiles.size() >0) {
                            changeSet.affectedFiles.each { changedFile ->

                                commitDetail += " ${changedFile.getPath()} \n"
                            }
                            commitDetail += "\n"
                        }
                    }
                }
        changeLogMessage += "commit(s) ${totalCommits} \n"
        changeLogMessage += "file change(s) ${fileChanges} \n"
        changeLogMessage += commitDetail
        return changeLogMessage
    }

/**
 * Send slack notification using slackSend required jenkins-slack-plugin
 *
 * @param execute boolean if false slack notification is not send
 * @param config object
 * @param config.buildStatus optional string expected values STARTED | SUCCESS | UNSTABLE | ABORTED | FAILURE | PROGRESS
 * @param config.buildMessage optional string to append header on slack message
 * @param config.changeLogMessage ?: optional string for custom change log message to attach
 * @param config.channel ?: optional string for sending notification to @individual or #group
 * @param config.extraAttachements optional array of attachment object refer https://api.slack.com/docs/message-attachments#fields
 */
    def void sender(boolean execute = true, config = [:]) {
        def utils = new io.polarpoint.utils.Utils()
        if (!execute) {
            return
        }
        if (!config.changeLogMessage) {
            config.changeLogMessage = this.getChangeLogMessage()
        }
        def buildStatus
        if (exceptionInBuild) {
            buildStatus = 'FAILURE'
        } else if (config.buildStatus) {
            buildStatus = config.buildStatus
        } else {
            buildStatus = pipeline.currentBuild.currentResult
        }
        def message = config.buildMessage ? config.buildMessage : "after ${pipeline.currentBuild.durationString.replace('and counting', '')}"
        def colorCode
        switch (buildStatus) {
            case 'STARTED':
                message = "${pipeline.currentBuild.rawBuild.getCauses().get(0).getShortDescription()}"
                colorCode = '#ccc'
                break
            case 'PASSED-TESTS':
                message = "Passed tests ${message}"
                colorCode = '#007bff'
                break
            case 'PASSED-QUALITY-TESTS':
                message = "Passed quality tests ${message}"
                colorCode = '#007bff'
                break
            case 'PASSED-INTEGRATION-TESTS':
                message = "Passed integration tests ${message}"
                colorCode = '#007bff'
                break
            case 'PROGRESS':
                message = "In Progress ${message}"
                colorCode = '#007bff'
                break
            case 'SUCCESS':
                message = "Success ${message}"
                colorCode = 'good'
                break
            case 'UNSTABLE':
                message = "Unstable ${message}"
                colorCode = 'warning'
                break
            case 'ABORTED':
                message = "Aborted ${message}"
                colorCode = '#ccc'
                break
            default:
                buildStatus = 'FAILURE'
                colorCode = 'danger'
                message = "Failed ${message}"
        }
        def attachmentPayload = [[
                                         fallback   : "${pipeline.env.JOB_NAME} execution #${pipeline.env.BUILD_NUMBER} - ${buildStatus}",
                                         author_link: "",
                                         author_icon: "",

                                         title_link : "${pipeline.env.JOB_URL}",
                                         color      : colorCode,
                                         fields     : [

                                                 [       title: "${pipeline.env.JOB_NAME}",
                                                         value: "<${pipeline.env.RUN_DISPLAY_URL}| #${pipeline.env.BUILD_NUMBER}> - ${message}",

                                                         short: false
                                                 ]
                                         ],
                                         footer     : "<${pipeline.env.JENKINS_URL}| Jenkins>",
                                         ts         : new Date().time / 1000
                                 ]]
        def totalCommits = pipeline.currentBuild.changeSets.size()
        if ((buildStatus == 'FAILURE' | buildStatus ==  'SUCCESS') && totalCommits > 0) {
            attachmentPayload[0].fields.add([
                    title: "Change log",
                    value: "${config.changeLogMessage}\n<${pipeline.env.BUILD_URL}/changes| Details>",
                    short: false
            ])
        }

        if (config.extraAttachements) {
            config.extraAttachements.each { extraAttachments ->
                attachmentPayload[0].fields.add(extraAttachments)
            }
        }

        def projectName = pipeline.currentBuild.fullProjectName
        def organisationName =  utils.parseGitHubOrganisation(projectName)

        if (organisationName != "" )
        {
            config.channel = organisationName+"-builds"
        }

        pipeline.slackSend(channel: config.channel, color: colorCode, attachments: new JsonBuilder(attachmentPayload).toPrettyString())
    }


    /**
     * Upload file using slackUploadFile required slack notification plugin
     *
     * @param config object
     * @param image String base64 string to send as PNG
     */
    def void fileSender(config = [:], String imageBase64) {
        def utils = new io.polarpoint.utils.Utils()
        if (!imageBase64)
        {
            return
        }

        def projectName = pipeline.currentBuild.fullProjectName
        def organisationName =  utils.parseGitHubOrganisation(projectName)

        if (organisationName != "" )
        {
            config.channel = organisationName+"-builds"
        }

        if (!config.filename) {
            config.filename =  "slack-image.png"
        }


           byte[]  fileByteArray =  Base64.getDecoder().decode(imageBase64);
           FileOutputStream fos = new FileOutputStream(new File(config.filename));
           fos.write(fileByteArray);
           fos.close()

            println("channel :"+config.channel)
            println("organisationName :"+organisationName)
            pipeline.slackUploadFile(channel: config.channel, filePath: config.filename, initialComment: projectName)

    }
}