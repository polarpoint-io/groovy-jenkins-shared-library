/*
 * Surj Bains  <surj@polarpoint.io>
 * ConfigurableGitRobHandlers
 */

package io.polarpoint.workflow.handlers

class ConfigurableGitRobHandlers implements Serializable, GitRobHandlers {

    String report
    String apply



    public ConfigurableGitRobHandlers(

            String report,
            String apply)
    {
        this.report = report
        this.apply = apply


    }


}


