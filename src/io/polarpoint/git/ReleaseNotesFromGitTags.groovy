package io.polarpoint.git
/*
 * Surj Bains  <surj@polarpoint.io>
 * ReleaseNotesFromGitTags
 * Creates files with release notes in Markup and HTML
 */

@Grab(group = 'org.pegdown', module = 'pegdown', version = '1.4.1')

import org.pegdown.PegDownProcessor
import groovy.text.SimpleTemplateEngine
import com.cloudbees.groovy.cps.NonCPS


class ReleaseNotesFromGitTags {
    @NonCPS
    def readTags() {
        def tags = []
        println("******************* ReleaseNotesFromGitTags readTags ******************")
        def proc = "git tag -l".execute().text
        println("ReleaseNotesFromGitTags readTags " +proc)
        def command = [ "/bin/bash", "-c", "git tag -l" ]
        def process = command.execute(); process.waitFor()
        def result = process.in.text.tokenize("\n")
        result.sort {}
        println("ReleaseNotesFromGitTags result " +result.toString())
        println("******************* ReleaseNotesFromGitTags readTags ******************")
        Collections.reverse(tags)
        tags
    }
    @NonCPS
    def readTagMessage(String tag) {
        def message = []
        println("readTagMessage tag :"+tag)
        def proc = "git cat-file tag $tag".execute()
        println("readTagMessage proc :"+proc)
        def startCollection = false
        proc.in.eachLine { line ->
            if (line.isEmpty()) {
                startCollection = true
            }
            if (startCollection) {
                message += line
            }
        }
        proc.err.eachLine { line -> println line }
        message
    }

    @NonCPS
    def releaseNotes(String application, String templatePath) {
        def releaseNotes = new File(templatePath+'releaseNotes.md')
        releaseNotes.withWriter {writer ->
            writer.write("") //delete contents
        }
        def versions = ""

        def tags = readTags()
        tags.each { tag ->
            versions += "- [$tag](#$tag)\n"
        }

        tags.each { tag ->
            releaseNotes << "# ${tag}<a name='$tag'></a>\n"
            def message = readTagMessage(tag)
            message.each { releaseNotes << "$it\n" }
            releaseNotes << "\n"
        }


        def writer = new StringWriter()
        def pdp = new PegDownProcessor()
        def engine = new SimpleTemplateEngine()
        println 'Using template file at :'+templatePath
        def template = engine.createTemplate(new File(templatePath +"releaseNotes.tpl"))
        def daten = [releaseNotes: pdp.markdownToHtml(new File(templatePath+"releaseNotes.md").text), application: application, versions: pdp.markdownToHtml(versions)]

        def ergebnis = template.make(daten)
        new File(templatePath+'releaseNotes.html').withWriter { w ->
            w.write(ergebnis)
        }
    }
}