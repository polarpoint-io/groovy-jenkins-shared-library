
package io.polarpoint.utils

import groovy.json.*
import io.polarpoint.utils.Utils
import java.io.Serializable
import gui.ava.html.image.generator.HtmlImageGenerator;
import java.io.File;

@Grapes(
        @Grab(group='gui.ava', module='html2image', version='2.0.1')
)


class HtmlImageGenerator implements Serializable {
    private def pipeline
    private def exceptionInBuild
    private boolean hasAnsiSupport
    private boolean disableAnsi;

    HtmlImageGenerator(pipeline, disableAnsi = false) {
        this.pipeline = pipeline
        this.disableAnsi = disableAnsi
        try {
            Class.forName('hudson.plugins.ansicolor.AnsiColorBuildWrapper', false, pipeline.getClass().getClassLoader())
            this.hasAnsiSupport = true
        } catch (java.lang.ClassNotFoundException e) {
            this.hasAnsiSupport = false
        }
    }

    def void imager(config) {
        this.imager(true, config)
    }

/**
 * Generate image from html file
 *
 * @param config object
 */
    def void imager(boolean execute = true, config = [:]) {
        def utils = new io.polarpoint.utils.Utils()
        if (!execute) {
            return
        }
        if (!config.uri) {
            return
        }
        if (!config.imageName) {
            return
        }
        HtmlImageGenerator imageGenerator = new HtmlImageGenerator();
        String uri = new File(config.uri).toURI().toString();
        imageGenerator.loadUrl(uri);
        imageGenerator.saveAsImage(config.imageName);

        pipeline.slackUploadFile(channel: config.channel, color: colorCode, filepath: config.imageName)
    }
}