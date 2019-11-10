package io.polarpoint.git

import com.cloudbees.groovy.cps.NonCPS

class TableFormatter {

    List<String> refs
    String repo

    @NonCPS
    def makeHtmlTable() {

        def writer = new StringWriter()
        def markup = new groovy.xml.MarkupBuilder(writer)
        def css_style =
                '''
table {
  table-layout: fixed;
  width: 80%;
  border-collapse: collapse;
  border: 3px solid purple;
}

h2 {
    color: #006400;
}
thead th:nth-child(1) {
  width: 30%;
}

thead th:nth-child(2) {
  width: 20%;
}

thead th:nth-child(3) {
  width: 15%;
}

thead th:nth-child(4) {
  width: 35%;
}

th, td {
  padding: 10px;
}
'''
        markup.html {
            delegate.head {
                delegate.style(type: "text/css", css_style)

            }
            delegate.body {
                delegate.h2('This is a test email (for now)')
                delegate.h2("Branches in ${repo} are scheduled to be deleted. ")
                delegate.p (value: '''You can delete remote branches with: git push origin --delete <branch>''')
                delegate.table {
                    delegate.tr {
                        delegate.th('Name')
                        delegate.th('Branch')
                        delegate.th('Last Commit Date')
                    }
                    for (line in refs) {
                        delegate.tr {
                            delegate.td(line[1])
                            delegate.td(line[3])
                            delegate.td(line[4])
                        }

                    }
                }
            }
        }

        return writer.toString()
    }
}