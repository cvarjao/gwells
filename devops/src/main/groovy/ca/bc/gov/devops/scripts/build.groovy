package ca.bc.gov.devops.scripts

import groovy.transform.BaseScript
import ca.bc.gov.devops.helpers.Base
import ca.bc.gov.devops.helpers.OpenShiftBuildHelper

@BaseScript Base _super

def config = new ConfigSlurper('dev').parse(this.getClass().getResource("config.groovy"))

new OpenShiftBuildHelper(config).build()

println 'Done!!'
