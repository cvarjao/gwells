import groovy.transform.BaseScript
import ca.bc.gov.devops.OpenShiftBuildHelper

@BaseScript ca.bc.gov.devops.Base _super

def config = new ConfigSlurper('dev').parse(new File("config.groovy").toURI().toURL())

println config.app.git

loadBuildTemplates(config)

applyBuildConfig(config)

new OpenShiftBuildHelper(config).build()


//println new groovy.json.JsonBuilder(config.app.templates.build).toPrettyString()


//oc('version')

println 'Done!!'