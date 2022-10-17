package io.polarpoint.utils



import java.util.regex.*
/**
 * Class to handle finding a folder string
 */
class DirFinder {

    static def pattern = Pattern.compile("/(libs)[w.\/-]{1,253}$(?<![.\/])")
    String dirName


    // find uuid
    static def find(text) {
        def matcher = pattern.matcher(text);
        if (matcher.find()) {
            new DirFinder(dirName: matcher.group(2))
        } else {
            echo "Couldn't find directory  :" + text
            new DirFinder(dirName: "0")
        }
    }


}


