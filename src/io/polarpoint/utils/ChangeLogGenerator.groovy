
package io.polarpoint.utils

import groovy.json.*
import io.polarpoint.utils.Utils
import java.io.Serializable
import java.io.File;


class ChangeLogGenerator implements Serializable {
    private def pipeline
    private def exceptionInBuild
    private boolean hasAnsiSupport
    private boolean disableAnsi;

    ChangeLogGenerator(pipeline, disableAnsi = false) {
        this.pipeline = pipeline
        this.disableAnsi = disableAnsi
        try {
            Class.forName('hudson.plugins.ansicolor.AnsiColorBuildWrapper', false, pipeline.getClass().getClassLoader())
            this.hasAnsiSupport = true
        } catch (java.lang.ClassNotFoundException e) {
            this.hasAnsiSupport = false
        }
    }

    def void changelog(config) {
        this.changelog(true, config)
    }

/**
 * Generate CHANGELOG.md
 *
 * @param config object
 */
    def void changelog(boolean execute = true, config = [:]) {
        if (!execute) {
            return
        }
        if (!config.uri) {
            return
        }


    }
}