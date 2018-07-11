import groovy.transform.BaseScript
import ca.bc.gov.devops.OpenShiftCleanupHelper

@BaseScript ca.bc.gov.devops.Base _super

def config = new ConfigSlurper('dev').parse(new File("config.groovy").toURI().toURL())

new OpenShiftCleanupHelper(config).build()

println 'Done!!'
