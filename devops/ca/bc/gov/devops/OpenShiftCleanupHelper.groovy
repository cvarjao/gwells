
package ca.bc.gov.devops

import ca.bc.gov.devops.OpenShiftHelper

class OpenShiftCleanupHelper extends OpenShiftHelper{
    def config
    //[object:null, phase:'New', buildName:null, builds:0, dependsOn:[], output:[from:[kind:''], to:['kind':'']] ]
    //Map cache = [:] //Indexed by key
    public OpenShiftCleanupHelper(config){
        this.config=config
    }

    private List loadCleanupTemplates(Map config, String phase){
        Map parameters =[
                'NAME_SUFFIX':config.app[phase].suffix,
                'ENV_NAME': config.app[phase].name,,
                'BUILD_ENV_NAME': config.app[phase].name,
                'SOURCE_REPOSITORY_URL': config.app.git.uri,
                'SOURCE_REPOSITORY_REF': config.app.git.ref
        ]

        return loadTemplates(config, config.app.build, parameters)
    }

    private cleanupByPhase(List templates, String phase){
        // remove all resources tagged with the specified env-name
        // println "delete all -l env-name=${config.app[phase].name} -n ${config.app[phase].namespace}"
        oc(['delete', 'all', '-l', "env-name=${config.app[phase].name}", '-n', "${config.app[phase].namespace}"])

        if('build'.equalsIgnoreCase(phase)){
            // remove tagged images in shared image streams
            templates.each { Map template ->
                template.objects.each { object ->
                    if ('ImageStream'.equalsIgnoreCase(object.kind)
                        && 'true'.equalsIgnoreCase(object.metadata.labels['shared'])
                        && !'true'.equalsIgnoreCase(object.metadata.labels['base-image'])){
                        // println "tag ${object.metadata.name}:${config.app[phase].name} -d -n ${config.app[phase].namespace}" 
                        oc(['tag', "${object.metadata.name}:${config.app[phase].name}", '-d', '-n', "${config.app[phase].namespace}"])
                    }
                }
            }
        }
    }

    public void cleanup(List phases){
        java.time.Instant startInstant = java.time.Instant.now()
        java.time.Duration duration = java.time.Duration.ZERO

        println 'Cleaning up...'

        for (phase in phases) {
            println "Processing '${phase}'"
            List templates = loadCleanupTemplates(config, phase)
            cleanupByPhase(templates, phase)
        }

        duration = java.time.Duration.between(startInstant, java.time.Instant.now())
        println "Elapsed Seconds: ${duration.getSeconds()} (max = ${config.app.build.timeoutInSeconds})"
    }
}