import groovy.transform.BaseScript
import ca.bc.gov.devops.OpenShiftDeploymentHelper

@BaseScript ca.bc.gov.devops.Base _super

def config = new ConfigSlurper('dev').parse(new File("config.groovy").toURI().toURL())


//println config.app.deployment.namespace
new OpenShiftDeploymentHelper(config).deploy()

println 'Done!!'
