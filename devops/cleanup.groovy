import groovy.transform.BaseScript
import ca.bc.gov.devops.OpenShiftCleanupHelper

@BaseScript ca.bc.gov.devops.Base _super

def config = new ConfigSlurper('dev').parse(new File("config.groovy").toURI().toURL())

Map phases = ['bc': ['build'], 'dc': ['deployment'], 'all': ['build', 'deployment']]
if(!args || !phases.keySet().contains(args[0].toLowerCase())){
  println 'Please specify a valid phase to clean up [build/deployment/all]'
  System.exit(1)
}

new OpenShiftCleanupHelper(config).cleanup(phases.get(args[0]))

println 'Done!!'
