import groovy.cli.commons.CliBuilder

import ca.bc.gov.devops.helpers.OpenShiftBuildHelper
import ca.bc.gov.devops.helpers.OpenShiftDeploymentHelper
import ca.bc.gov.devops.helpers.OpenShiftCleanupHelper

/**
 * Returns the ConfigObject representing the base configuration, inherited by all projects.
 * @return ConfigObject
 */
private static ConfigObject getBaseConfig() {
    ConfigObject baseConfig = new ConfigSlurper().parse(new File("baseconfig.groovy").toURI().toURL())
    return baseConfig
}

/**
 * Returns the ConfigObject representing project-specific configuration and overrides
 * @param envName The name of the environment for which to grab the configuration (e.g.: dev)
 * @return ConfigObject
 */
private static ConfigObject getProjectConfig(String envName, Map bindings) {
    ConfigSlurper cfgSlurper = new ConfigSlurper(envName)
    cfgSlurper.setBinding(bindings)
    return cfgSlurper.parse(new File("config.groovy").toURI().toURL())
}

/**
 * Returns a ConfigObject which includes the project base configuration plus any project specific configurations
 * and overrides. Produces the same result as <code>baseconfig.merge(projectconfig).</code>
 * @param envName The name of the environment for which to grab the configuration (e.g.: dev)
 * @return ConfigObject
 */
private static ConfigObject getMergedConfig(String envName) {
    def baseConfig = getBaseConfig()
    return getProjectConfig(envName, [ CHANGE_ID : baseConfig.app.git.changeId])
}

/**
 * Returns a list containing the environments to be processed. If app.environment overrides have been set in the
 * project config, they will be used. Otherwise, the default environments in the base config will be returned.
 * @return
 */
private static List getEnvironmentList() {
    ConfigObject baseConfig = new ConfigSlurper().parse(new File("baseconfig.groovy").toURI().toURL())
    ConfigObject projectConfig = new ConfigSlurper().parse(new File("config.groovy").toURI().toURL())
    baseConfig = baseConfig.merge(projectConfig)
    return baseConfig.app.environments
}

/**
 * // TODO
 * @param args
 * @return
 */
static def run(args){
    def cli = new CliBuilder(usage:'openshiftRun.groovy <action>')
    cli.h(longOpt:'help', 'Show usage information')
    cli._(longOpt:'action', args:1, argName:'action', 'The task to be run on OpenShift: build, deploy, cleanup (must be logged in)')

    def options = cli.parse(args)
    if (options.arguments().size() < 1 || options.h) {
        cli.usage()
        return
    }

    def baseConfig = this.getBaseConfig()

    def action = options.arguments().get(0).toLowerCase()
    switch (action) {
        case 'build':
            println 'Starting build process'
            this.getEnvironmentList().each {
                println "Processing '${it}' environment"
                def envConfig = getMergedConfig("${it}")
                new OpenShiftBuildHelper(envConfig).build()
            }
            break
        case 'deploy':
            println 'Starting deploy process'
            this.getEnvironmentList().each {
                println "Processing '${it}' environment"
                def envConfig = getMergedConfig("${it}")
                new OpenShiftDeploymentHelper(config).deploy()
            }
            break
        case 'cleanup':
            println 'Starting cleanup process'
            this.getEnvironmentList().each {
                println "Processing '${it}' environment"
                def envConfig = getMergedConfig("${it}")
                new OpenShiftCleanupHelper(config).cleanup()
            }
            break
        default:
            println 'Unrecognized options/argument. Please run openshiftRun.groovy --help to display usage information.'
            break
    }
}

run(args)