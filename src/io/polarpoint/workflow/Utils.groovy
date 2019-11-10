package io.polarpoint.workflow

import com.cloudbees.groovy.cps.NonCPS
import org.codehaus.groovy.runtime.StackTraceUtils


@NonCPS
def static StackTraceUtils stackTrace(Exception exception)
{
    return  org.codehaus.groovy.runtime.StackTraceUtils.sanitize(new Exception(exception)).printStackTrace()
}

