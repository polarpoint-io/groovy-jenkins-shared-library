/*
 * Peter Beverley <peter.beverley@uk.fujitsu.com>
 * invokeConfluence
 * Update pre-existing confluence pages with an audit of commit/build results
 * 
 */

// Get external dependencies
import com.google.gson.Gson
@Grab(group = 'org.jsoup', module = 'jsoup', version = '1.11.2')
@Grab(group = 'com.google.code.gson', module = 'gson', version = '2.8.2')
@GrabExclude('commons-lang:commons-lang')

// Import required libraries
import groovy.json.JsonSlurperClassic
import io.polarpoint.utils.Utils
import io.polarpoint.workflow.ConfigurationContext
import io.polarpoint.workflow.Confluence
import org.jsoup.Jsoup

def call(String targetBranch, ConfigurationContext context, String buildResult) {

    def server = "https://pol-hngt-devops.mycnets.com/"
    def basePath = "confluence"
    def credentials = "confluence_user"
    def space = 'HNGT'
    def base64Creds

    def utils = new Utils()

    if (utils.getCredentialsById(credentials)) {
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: credentials, usernameVariable: 'CONF_USER', passwordVariable: 'CONF_PASS']]) {
            base64Creds = (CONF_USER + ':' + CONF_PASS).bytes.encodeBase64().toString()
        }
    }

    final int id = 0
    final int ver = 1

    String pageTitle = context.config.application + ' - Build Log'
    ArrayList jobResults = []
    def changeLogSets = currentBuild.changeSets
    for (logSet in changeLogSets) {
        for (entry in logSet.items) {
            ArrayList jobResult = ["${new Date(entry.timestamp)}", entry.author, entry.msg, BRANCH_NAME, entry.commitId.take(7), BUILD_NUMBER, BUILD_URL, buildResult]
            jobResults << jobResult
        }
    }

    // Get current page content and update with new details
    def updatePage = {
        addRows ->

            // Get the page content
            def pgContent = Confluence.getContent(
                    server,
                    basePath,
                    base64Creds,
                    pgInfo[id]
            )

            // Parse the returned page html
            def tblHtml = Jsoup.parse(pgContent)

            // Determine number of rows, we will use as new row number (base 0)
            int newRownum = tblHtml.getElementsByTag('tr').size()

            // Create a new row by cloning last row in table
            def newRow = tblHtml.getElementsByTag('tr')[newRownum - 1].clone()

            // Create an object containing the row cells
            def newCells = newRow.children()

            // Determine the number of cells in a row
            int numCells = newCells.size()

            // Iterate over the remaining cells and insert required values
            for (addRow in addRows) {
                for (i = 0; i < numCells; i++) {

                    // Remove superfluous whitespace
                    addRow[i] = addRow[i].toString().trim()

                    // If the cell contains a URL insert an a tag with required attributes
                    if (addRow[i].matches("http(.*)")) {
                        newCells[i].text("")
                        def newLink = newCells[i].appendElement("a")
                        newLink.text(addRow[i].split("/job/", 2)[1])
                        newLink.attr("href", addRow[i])
                        newLink.attr("target", "_blank")
                    } else {
                        // Otherwise just dump the contents in the cell
                        newCells[i].text(addRow[i])
                    }
                }

                // Assume build status is in the last cell, set row color based on status
                switch (newRow.children().last().text()) {
                    case ~/^FAILURE$/:
                        newRow.attr("style", "background-color:red")
                        break
                    case ~/^UNSTABLE$/:
                        newRow.attr("style", "background-color:yellow")
                        break
                    case ~/^SUCCESS$/:
                        newRow.attr("style", "background-color:lime")
                        break
                }

                // Append our new row to the existing table after converting header row to standard in case we cloned the header
                tblHtml.select('tbody').append(newRow.outerHtml().replace('th', 'td'))
            }
            // Extract the table HTML from the page
            def newTable = tblHtml.select('table')

            // Define skeleton JSON required to modify a confluence page
            def modPgJson = new JsonSlurperClassic().parseText('{"id": "", "type": "page","title": "","space": {"key": ""},"body": {"storage": {"value": "","representation": "storage"}},"version":{"number":0}}')

            // Update the skeleton JSON with our new values and set the content to our new table
            modPgJson.id = pgInfo[id]
            modPgJson.version.number = pgInfo[ver] + 1
            modPgJson.title = pageTitle
            modPgJson.space.key = space
            modPgJson.body.storage.value = newTable.outerHtml()
            modPgJson = new Gson().toJson(modPgJson, HashMap.class)

            // Update the confluence page with our new version
            Confluence.setContent(
                    server,
                    basePath,
                    base64Creds,
                    space,
                    pgInfo[id],
                    modPgJson
            )

            // Find page id and version
            pgInfo = Confluence.findPage(
                    server,
                    basePath,
                    base64Creds,
                    space,
                    pageTitle
            )

            // Check if we found our page
            if (pgInfo != null) {
                // Page found, update it
                // Rows to add to table
                // for (jobResult in jobResults) {
                updatePage(jobResults)
                // }
            } else {
                // No page found
                echo "No results found for that criteria"
            }
    }
}



return this;
