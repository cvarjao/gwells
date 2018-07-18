package ca.bc.gov.devops.scripts

import groovy.transform.BaseScript
import ca.bc.gov.devops.helpers.Base
import ca.bc.gov.devops.helpers.OpenShiftDeploymentHelper

@BaseScript Base _super

def config = new ConfigSlurper('dev').parse(this.getClass().getResource("config.groovy"))

new OpenShiftDeploymentHelper(config).deploy()

println 'Done!!'
