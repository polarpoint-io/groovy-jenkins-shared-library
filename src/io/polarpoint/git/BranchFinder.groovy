package io.polarpoint.git

import com.cloudbees.groovy.cps.NonCPS
import groovy.time.TimeCategory

class BranchFinder {

    @NonCPS
    def findStaleBranches(allRefs, days) {

        def expireDate
        def refsToDelete = []
        def allRefsList = []

        if (allRefs.size() > 0) {

            // reflines is a list of lists for easy handling
            allRefs.each { ref ->
                def s = ref.split(',')
                allRefsList << s
            }
            use(TimeCategory) {
                expireDate = new Date() - days
            }
            println 'Finding branches older than' + expireDate.toString()

            // iterate through refs looking for dates older than expireDate
            // got to be a better way to do this!
            for (line in allRefsList) {
                if (Date.parse("yyyy-MM-dd", line[4]).before(expireDate)) {

                    line[3] = line[3].replaceFirst(/^origin\//, "")
                    if (line[3] ==~ /(master|development|develop|dev)/) {
                        continue
                    }

                    refsToDelete << line
                }
            }

            return refsToDelete

        } else {
            println '''Can't find any branches'''
            return []
        }
    }
}
