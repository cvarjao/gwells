package ca.bc.gov.devops

import static OpenShiftHelper.oc
import static OpenShiftHelper.ocProcess
import static OpenShiftHelper.gitHashAsBlobObject
import static OpenShiftHelper.getVerboseLevel

abstract class Base extends Script {
    static String CMD_OC='oc'
    static String CMD_GIT='git'



    List ocProcessParameters(List args){
        List parameters= []
        StringBuffer stdout= new StringBuffer()
        StringBuffer stderr= new StringBuffer()

        oc(['process'] + args + ['--parameters=true'], stdout, stderr)
        stdout.eachLine {line, number ->
            if (number > 0){
                parameters.add(line.tokenize()[0])
            }
        }
        return parameters
    }

    Map _exec(List args){
       return _exec( _args.execute())
    }

    Map _exec(java.lang.Process proc){
        return _exec(proc, new StringBuffer(), new StringBuffer())
    }

    Map _exec(java.lang.Process proc, StringBuffer stdout, StringBuffer stderr){
        proc.waitForProcessOutput(stdout, stderr)
        int exitValue= proc.exitValue()
        Map ret = ['out': stdout, 'err': stderr, 'status':exitValue]
        return ret
    }


    void loadBuildTemplates(config){
        String gitRemoteUri=config.app.git.uri
        println 'Reading Build Templates ...'
        config.app.templates.build.each { template ->
            if (getVerboseLevel() >= 2) println template.file

            //Load Template
            Map templateObject = new groovy.json.JsonSlurper().parseFile(new File(template.file), 'UTF-8')
            //Normalize template and calculate hash
            templateObject.objects.each { it ->
                it.metadata.labels=it.metadata.labels?:[:]
                it.metadata.annotations=it.metadata.annotations?:[:]
                it.metadata.namespace = it.metadata.namespace?:config.app.build.namespace

                //Everybody gets a config hash! hooray!
                it.metadata.labels['hash']= gitHashAsBlobObject(groovy.json.JsonOutput.toJson(it))
            }

            //println templateObject.parameters
            //List parameterNames = ocProcessParameters(['-f', template.file])

            List params=['-n', config.app.build.namespace]

            templateObject.parameters.each { param ->
                String name = param.name
                String value=''
                if ('NAME_SUFFIX'.equalsIgnoreCase(name)){
                    value=config.app.build.suffix
                }else if ('ENV_NAME'.equalsIgnoreCase(name)){
                    value=config.app.build.name
                }else if ('SOURCE_REPOSITORY_URL'.equalsIgnoreCase(name)){
                    value=gitRemoteUri
                }else if ('SOURCE_REPOSITORY_REF'.equalsIgnoreCase(name)){
                    value=config.app.git.ref
                }
                params.addAll(['-p', "${name}=${value}"])
            }

            Map ocRet=ocProcess(templateObject, params)
            
            //System.exit(1)

            def objects=new groovy.json.JsonSlurper().parseText(ocRet.out.toString())

            objects.items.each {
                it.metadata.labels=it.metadata.labels?:[:]
                it.metadata.annotations=it.metadata.annotations?:[:]
                //normalize to explicit namespace references (it makes things easier)
                it.metadata.namespace = it.metadata.namespace?:config.app.build.namespace

                if ('BuildConfig'.equalsIgnoreCase(it.kind)){
                    if (!it.spec.completionDeadlineSeconds) println "WARN: Please set ${key(it)}.spec.completionDeadlineSeconds"
                    
                    if (getVerboseLevel() >= 4) println "${it.kind}/${it.metadata.name} - ${it.spec.source.contextDir}"
                    //it.metadata.labels['hash']= gitHashAsBlobObject(groovy.json.JsonOutput.toJson(it))

                    it.metadata.labels['build-config.name'] = it.metadata.name
                    //normalize to explicit namespace references (it makes things easier)
                    it.spec.output.to.namespace = it.spec.output.to.namespace?:config.app.build.namespace
                    if (it.spec?.source?.images != null){
                        for (Map image:it.spec.source?.images){
                            image.from.namespace=image.from.namespace?:config.app.build.namespace
                        }
                    }

                    if (gitRemoteUri.equalsIgnoreCase(it.spec.source?.git?.uri) && it.spec?.source?.contextDir != null){
                        it.metadata.labels['tree-hash'] = ['git', 'log', '-1', '--pretty=format:%T', '--', "../${it.spec?.source?.contextDir}"].execute().text
                    }
                    it.spec.triggers = [] //it.spec.triggers.findAll({!'ConfigChange'.equalsIgnoreCase(it.type)})
                    if (it.spec.strategy.sourceStrategy){
                        it.spec.strategy.sourceStrategy.env=it.spec.strategy.sourceStrategy.env?:[]
                        it.spec.strategy.sourceStrategy.env.add(['name':"OPENSHIFT_BUILD_CONFIG_HASH", 'value':"${it.metadata.labels['hash']}"])
                        it.spec.strategy.sourceStrategy.env.add(['name':"OPENSHIFT_BUILD_TREE_HASH", 'value':"${it.metadata.labels['tree-hash']}"])
                    }else if (it.spec.strategy.dockerStrategy){
                        it.spec.strategy.dockerStrategy.env=it.spec.strategy.dockerStrategy.env?:[]
                        it.spec.strategy.dockerStrategy.env.add(['name':"OPENSHIFT_BUILD_CONFIG_HASH", 'value':"${it.metadata.labels['hash']}"])
                        it.spec.strategy.dockerStrategy.env.add(['name':"OPENSHIFT_BUILD_TREE_HASH", 'value':"${it.metadata.labels['tree-hash']}"])
                    }
                    //println groovy.json.JsonOutput.toJson(it.spec.triggers)
                }else if ('ImageStream'.equalsIgnoreCase(it.kind)){
                    if (getVerboseLevel() >= 4) println "${it.kind}/${it.metadata.name}"
                    it.metadata.labels['image-stream.name'] = it.metadata.name
                }else{
                    //openshift.io/build-config.name
                    if (getVerboseLevel() >= 4) println "${it.kind}/${it.metadata.name}"
                }
            }
            //println objects
            template.objects=objects.items
        }
    } //end function

    void applyBuildConfig(config){
        println 'Applying Build Templates ...'
        config.app.templates.build.each { template ->
            template.objects.each { object ->
                object.metadata.labels['app-name'] = config.app.name
                if (!'true'.equalsIgnoreCase(object.metadata.labels['shared'])){
                    object.metadata.labels['env-name'] = config.app.build.name
                    object.metadata.labels['app'] =  object.metadata.labels['app-name'] + '-' + object.metadata.labels['env-name']
                }
            }

            String json=new groovy.json.JsonBuilder(['kind':'List', 'apiVersion':'v1', 'items':template.objects]).toPrettyString()

            /*
            new File('tmp.json').withWriter('UTF-8') { writer ->
                writer.write(json)
            }
            */

            def proc = ['oc', 'apply', '-f','-', '-n', 'csnr-devops-lab-tools'].execute()
            OutputStream out = proc.getOutputStream();
            out.write(json.getBytes());
            out.flush();
            out.close();

            Map ret= _exec(proc)
            
            if (ret.status != 0) {
                println ret
                System.exit(ret.status)
            }
        }
    } //end applyBuildConfig

    String ukey(Map object){
        return "${object.metadata.namespace}/${object.kind}/${object.metadata.name}"
    }

    String key(Map object){
        return "${object.kind}/${object.metadata.name}"
    }

} //end class