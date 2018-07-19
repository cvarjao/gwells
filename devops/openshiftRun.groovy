import groovy.cli.commons.CliBuilder

static def run(args){
    def cli = new CliBuilder(usage:'openshiftRun.groovy <action>')
    cli.h(longOpt:'help', 'Show usage information')
    cli._(longOpt:'action', args:1, argName:'action', 'The task to be run on OpenShift: build, deploy, cleanup (must be logged in)')

    def options = cli.parse(args)
    if (options.arguments().size() == 0 || options.h) {
        cli.usage()
        return
    }

    def action = options.arguments().get(0).toLowerCase()
    switch (action) {
        case 'build':
            println 'Starting build process'

            break
        case 'deploy':
            println 'Starting deploy process'
            break
        case 'cleanup':
            println 'Starting cleanup process'
            break
        default:
            println 'Unrecognized options/argument. Please run openshiftRun.groovy --help to display usage information.'
            break
    }
}

run(args)