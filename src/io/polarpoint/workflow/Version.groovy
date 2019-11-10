package io.polarpoint.workflow



import java.util.regex.*
/**
 * Class to handle version strings
 */
class Version {

    static def pattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)")
    String major, minor, patch

    // parse version
    static def parseVersion(text) {
        def matcher = pattern.matcher(text);
        if (matcher.find()) {
            new Version(major: matcher.group(1), minor: matcher.group(2), patch: matcher.group(3))
        } else {
            echo " Version wasn't parsed from :" + text
            new Version(major: "0", minor: "0", patch: "0")
        }
    }

    String toString() {
        String.format("%d,%d,%d",major.toInteger(), minor.toInteger(), patch.toInteger())
    }

}


