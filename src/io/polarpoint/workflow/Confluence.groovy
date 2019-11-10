package io.polarpoint.workflow

import groovy.json.JsonOutput

// Get external dependencies


@Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.1')
@GrabExclude('commons-lang:commons-lang')


// Import required libraries
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient

import static groovyx.net.http.ContentType.*

// Error handler for http-builder RESTClient
def httpErr(HttpResponseException err) {
    r = err.response
    println("Success: $r.success")
    println("Status:  $r.status")
    println("Reason:  $r.statusLine.reasonPhrase")
    println("Content: \n${JsonOutput.prettyPrint(JsonOutput.toJson(r.data))}")
}

// Find page in confluence based on title and space, we assume a single match so just default to results[0]
// return id and version number of matching page
def findPage(String conflServer, String basePath, String encCreds, String conflSpace, String srchTitle) {
    def https = new RESTClient(conflServer)
    try {

        def resp = https.get(
                path: basePath + 'rest/api/content/',
                headers: ['Authorization': 'Basic ' + encCreds],
                requestContentType: JSON,
                query: [title: srchTitle, spaceKey: conflSpace, expand: 'version']
        )
        if (resp.data.results.size() > 0) {
            [resp.data.results[0].id, resp.data.results[0].version.number]
        }
    } catch (HttpResponseException e) {
        httpErr(e)
    }
}

// Get the content of the confluence page referenced by docID
static def getContent(String conflServer, String basePath, String encCreds, String docID) {
    def https = new RESTClient(conflServer)
    try {
        def resp = https.get(
                path: basePath + 'rest/api/content/' + docID,
                headers: ['Authorization': 'Basic ' + encCreds],
                requestContentType: JSON,
                query: [expand: 'body.storage.content']
        )
        resp.data.body.storage.value
    } catch (HttpResponseException e) {
        httpErr(e)
    }
}

// Set the content of the confluence page referenced by docID

def setContent(String conflServer, String basePath, String encCreds, String conflSpace, String docID, String newPage) {
    def https = new RESTClient(conflServer)
    try {
        def resp = https.put(
                path: basePath + 'rest/api/content/' + docID,
                headers: ['Authorization': 'Basic ' + encCreds],
                requestContentType: JSON,
                body: newPage
        )
        resp.data
    } catch (HttpResponseException e) {
        httpErr(e)
    }
}

