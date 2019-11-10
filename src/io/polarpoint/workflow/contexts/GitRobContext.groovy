/*
 * Surj Bains  <surj@polarpoint.io>
 * GitRobContext
 */


package io.polarpoint.workflow.contexts

import io.polarpoint.workflow.handlers.ConfigurableGitRobHandlers



@SuppressWarnings('FieldName')
class GitRobContext implements Serializable {

    private static final long serialVersionUID = 0L
    final HashMap config
    final String application
    final ConfigurableGitRobHandlers configurableGitRobHandlers


    GitRobContext(String application, HashMap config) {
        this.application = application
        this.config = config

    }

    GitRobContext(String application, String configuration, String ws = null) {
        this.application = application
        this.config = (new HashMap(
                new groovy.json.JsonSlurperClassic().
                        parseText(configuration)
        ))



        def workspaceLibs = "${ws}@libs"

        def report = config.reporter ?: "${workspaceLibs}/pipeline-library/gitrob-pipeline/report/reporter.groovy"
        def apply = config.apply ?: "${workspaceLibs}/pipeline-library/gitrob-pipeline/apply/applier.groovy"


        this.configurableGitRobHandlers = new io.polarpoint.workflow.handlers.ConfigurableGitRobHandlers(
                report,
                apply)

    }
}


