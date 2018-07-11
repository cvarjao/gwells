
package ca.bc.gov.devops

class OpenShiftDeploymentHelper extends OpenShiftHelper{
    def config

    //[object:null, phase:'New', buildName:null, builds:0, dependsOn:[], output:[from:[kind:''], to:['kind':'']] ]
    //Map cache = [:] //Indexed by key
    
    public OpenShiftDeploymentHelper(config){
        this.config=config
    }

    private List loadDeploymentTemplates(){
        Map parameters =[
                'NAME_SUFFIX':config.app.deployment.suffix,
                'ENV_NAME': config.app.deployment.name,
                'BUILD_ENV_NAME': config.app.deployment.name
        ]
        return loadTemplates(config, config.app.deployment, parameters)
    }


    private void applyDeploymentConfig(Map deploymentConfig, List templates){
        println 'Preparing Deployment Templates ...'
        List errors=[]

        templates.each { Map template ->
            println "Preparing ${template.file}"
            template.objects.each { Map object ->
                println "Preparing ${key(object)}  (${object.metadata.namespace})"
                object.metadata.labels['app-name'] = config.app.name
                if (!'true'.equalsIgnoreCase(object.metadata.labels['shared'])){
                    object.metadata.labels['env-name'] = deploymentConfig.name
                    object.metadata.labels['app'] =  object.metadata.labels['app-name'] + '-' + object.metadata.labels['env-name']
                }
                String asCopyOf = object.metadata.annotations['as-copy-of']

                if ((object.kind == 'Secret' || object.kind == 'ConfigMap') &&  asCopyOf!=null){
                    Map sourceObject = ocGet([object.kind, asCopyOf,'--ignore-not-found=true',  '-n', object.metadata.namespace])
                    if (sourceObject ==  null){
                        errors.add("'${object.kind}/${asCopyOf}' was not found in '${object.metadata.namespace}'")
                    }else{
                        object.data=sourceObject.data
                    }
                }
            }
        }

        if (errors.size()){
            throw new RuntimeException("The following errors were found: ${errors.join(';')}")
        }

        templates.each { Map template ->
            println "Applying ${template.file}"
            Map ret= ocApply(template.objects, ['-n', deploymentConfig.namespace])
            if (ret.status != 0) {
                println ret
                System.exit(ret.status)
            }
        }

    } //end applyBuildConfig

    public void deploy(){
        List templates = loadDeploymentTemplates()
        applyDeploymentConfig(config.app.deployment, templates)
    }
}