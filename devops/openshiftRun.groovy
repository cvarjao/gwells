import groovy.cli.commons.CliBuilder
import groovy.util.ConfigSlurper

import ca.bc.gov.devops.helpers.OpenShiftBuildHelper
import ca.bc.gov.devops.helpers.OpenShiftDeploymentHelper
import ca.bc.gov.devops.helpers.OpenShiftCleanupHelper

static def run(args){
    def cli = new CliBuilder(usage:'openshiftRun.groovy <action> <envname>')
    cli.h(longOpt:'help', 'Show usage information')
    cli._(longOpt:'action', args:1, argName:'action', 'The task to be run on OpenShift: build, deploy, cleanup (must be logged in)')
    cli._(longOpt:'envName', args:1, argName:'envName', 'The identifier ro be used as label to identify the target objects')

    def options = cli.parse(args)
    if (options.arguments().size() < 2 || options.h) {
        cli.usage()
        return
    }

    def action = options.arguments().get(0).toLowerCase()
    switch (action) {
        case 'build':
            println 'Starting build process'
            def config = new ConfigSlurper('dev').parse(new File("config.groovy").toURI().toURL())
            new OpenShiftBuildHelper(config).build()
            break
        case 'deploy':
            println 'Starting deploy process'
            def config = new ConfigSlurper('dev').parse(new File("config.groovy").toURI().toURL())
            new OpenShiftDeploymentHelper(config).deploy()
            break
        case 'cleanup':
            println 'Starting cleanup process'
            def config = new ConfigSlurper('dev').parse(new File("config.groovy").toURI().toURL())
            new OpenShiftCleanupHelper(config).cleanup()
            break
        default:
            println 'Unrecognized options/argument. Please run openshiftRun.groovy --help to display usage information.'
            break
    }
}

run(args)