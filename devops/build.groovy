import groovy.transform.BaseScript
import ca.bc.gov.devops.OpenShiftBuildHelper

@BaseScript ca.bc.gov.devops.Base _super

def config = new ConfigSlurper('dev').parse(new File("config.groovy").toURI().toURL())

new OpenShiftBuildHelper(config).build()


println 'Done!!'