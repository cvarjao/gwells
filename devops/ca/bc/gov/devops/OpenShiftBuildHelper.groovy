
package ca.bc.gov.devops

import static ca.bc.gov.devops.OpenShiftHelper.key
import static ca.bc.gov.devops.OpenShiftHelper.oc
import static ca.bc.gov.devops.OpenShiftHelper.ocGet
import static ca.bc.gov.devops.OpenShiftHelper.ocApply
import static ca.bc.gov.devops.OpenShiftHelper.toJson
import static ca.bc.gov.devops.OpenShiftHelper.calculateChecksum
import static ca.bc.gov.devops.OpenShiftHelper.addBuildConfigEnv
import static ca.bc.gov.devops.OpenShiftHelper.getVerboseLevel

class OpenShiftBuildHelper{
    def config
    public OpenShiftBuildHelper(config){
        this.config=config
    }

    private boolean isNextToBuild(Map from, List pending, List processed){
        if (from.namespace != null){
            if (getVerboseLevel() >= 5) println '   (pick) no namespace'
            return true
        }else{
            for( Map bc2:pending){
                Map outputTo=bc2.spec.output.to
                if (getVerboseLevel() >= 5) println "   ${from} - ${key(bc2)}  - output -> ${outputTo}"
                if (outputTo.namespace == null && 'ImageStreamTag'.equalsIgnoreCase(outputTo.kind)){
                    if (outputTo.name.equalsIgnoreCase(from?.name)){
                        if (getVerboseLevel() >= 5) println '   (skip)'
                        return false
                    }
                }
            }
        }
        return true
    }

    Map imageStreamImageLookupCache=[:]
    private Map getImageStreamImage(String namespace, String name){
        String cacheKey="${namespace}/ImageStreamImage/${name}"
        Map imageStreamImage = imageStreamImageLookupCache[cacheKey]

        if (imageStreamImage == null){
            imageStreamImage=toJson(oc(['get', 'ImageStreamImage', name, '-o', 'json', '-n', namespace]).out)
            imageStreamImageLookupCache[cacheKey]=imageStreamImage
        }
        
        return imageStreamImage
    }
    public List getImageStreamTagByBuildHash(Map ref, String hash){
        if (!'ImageStreamTag'.equalsIgnoreCase(ref?.kind)){
            throw new RuntimeException("Expected kind='ImageStreamTag', but found kind='${ref?.kind}'")
        }
        Map images = [:]
        String namespace=ref.namespace?:config.app.build.namespace
        String imageStreamName=ref.name.split(':')[0]
        Map imageStreamTags=toJson(oc(['get', 'ImageStreamTag','-l',"image-stream.name=${imageStreamName}", '-o', 'json', '-n', namespace]).out)

        for (Map imageStreamTag:imageStreamTags.items){
            String imageName=imageStreamTag.image.metadata.name
            if (images[imageName]==null){
                Map imageStreamImage = getImageStreamImage(namespace, "${imageStreamName}@${imageName}")
                for (String imageEnv: imageStreamImage.image.dockerImageMetadata.'Config'.'Env'){
                    if ("_BUILD_HASH=${hash}".equalsIgnoreCase(imageEnv)){
                        images[imageName]=imageStreamImage.image.metadata
                        break;
                    }
                }
            }
        }

        return [] + images.values()
        //System.exit(1)
        /// /apis/image.openshift.io/v1/namespaces/csnr-devops-lab-tools/imagestreamimages/

        //oc get is/gwells-pr-719 -o json
        //status.tags[].items[].image

        //oc export isimage/gwells-pr-719@sha256:b05ed259a8336b69356a586a698dcf9e1d9563c09ef3e5d20a39fdc7bf7a6f86 -o json
        //image.dockerImageMetadata.ContainerConfig.Env[]
        // find 
    }

    public Map getImageStreamTag(Map ref){
        //if (ref == null) return null
        if (!'ImageStreamTag'.equalsIgnoreCase(ref.kind)){
            throw new RuntimeException("Expected kind='ImageStreamTag', but found kind='${ref?.kind}'")
        }
        
        Map ret=oc(['get', 'istag', ref.name, '--ignore-not-found=true', '-o', 'json', '-n', ref.namespace?:config.app.build.namespace])
        if (ret.out!=null && ret.out.length() > 0){
            return toJson(ret.out)
        }
        return null
    }

    boolean isSameImageStreamTagReference(Map ref1, Map ref2){
        if (
            (ref1.namespace?:config.app.build.namespace).equalsIgnoreCase(ref2.namespace?:config.app.build.namespace) &&
            (ref1.name).equalsIgnoreCase(ref2.name) &&
            (ref1.kind).equalsIgnoreCase(ref2.kind) &&
            'ImageStreamTag'.equalsIgnoreCase(ref1.kind)
        ){
            return true
        }
        return false
    }

    boolean isBuidActive(Map build){
        return 'New' == build.status.phase || 'Pending' == build.status.phase || 'Running' == build.status.phase
    }
    boolean isBuidSuccessful(Map build){
        return 'Complete' == build.status.phase
    }
    boolean allBuildsSuccessful(List builds){
        //for a list of possible status use: oc explain build.status.phase
        boolean allComplete = true
        for (Map build:builds){
            if (!isBuidSuccessful(build)){
                allComplete=false
                println "Waiting for ${key(build)} status.phase = '${build.status.phase}' , expected 'Complete'"
            }
        }
        return allComplete
    }

    public List getImageStreamTags(List references){
        List tags = []
        for (Map imageReference:references){
            Map imageStreamTag=getImageStreamTag(imageReference)
            if (imageStreamTag!=null){
                tags.add(imageStreamTag)
            }
        }
        return tags
    }

    List getBuildConfigFromImageStreamTagReferences(Map bc){
        Map lookup=[:]
        Map from=(bc.spec?.strategy?.dockerStrategy?.from)?:(bc.spec?.strategy?.sourceStrategy?.from)
        from.namespace=from.namespace?:config.app.build.namespace
        lookup["${from.namespace}/${from.kind}/${from.name}"]=from

        if (bc.spec?.source?.images != null){
            for (Map image:bc.spec.source?.images){
                image.from.namespace=image.from.namespace?:config.app.build.namespace
                lookup["${image.from.namespace}/${image.from.kind}/${image.from.name}"]=image.from
            }
        }
        return [] + lookup.values()
    }

    List getLatestRelatedBuilds(bc){
        List builds=[]
        Map dependencies=[:]

        //println "Checking related builds of ${key(bc)}"
        Map from=(bc.spec?.strategy?.dockerStrategy?.from)?:(bc.spec?.strategy?.sourceStrategy?.from)
        dependencies["${from.namespace?:config.app.build.namespace}/${from.kind}/${from.name}"]=from
        if (bc.spec?.source?.images != null){
            for (Map image:bc.spec.source?.images){
                dependencies["${image.from.namespace?:config.app.build.namespace}/${image.from.kind}/${image.from.name}"]=image.from
            }
        }

        Map buildConfigs=ocGet(['bc', '-l', "app=${bc.metadata.labels['app']}", '-n', config.app.build.namespace])
        if (buildConfigs!=null){
            for (Map buildConfig:buildConfigs.items){
                //println "Checking ${key(buildConfig)}"
                Map imageStreamTagReference=buildConfig.spec.output.to
                if (dependencies["${imageStreamTagReference.namespace?:config.app.build.namespace}/${imageStreamTagReference.kind}/${imageStreamTagReference.name}"]!=null){
                    if (buildConfig.status.lastVersion > 0){
                        Map build=ocGet(["build/${buildConfig.metadata.name}-${buildConfig.status.lastVersion}", '-n', config.app.build.namespace])
                        builds.add(build)
                    }
                }
            }
        }

        //println "${key(bc)} related builds ${builds}"
        return builds
    }

    public String calculateBuildHash(Map bc){
        Map record = ['images':[:]]
        Map fromImageStreamTag = (bc.spec?.strategy?.dockerStrategy?.from)?:(bc.spec?.strategy?.sourceStrategy?.from)

        record['buildConfig']=bc.metadata.labels['hash']
        record['source']=bc.metadata.labels['tree-hash']

        record['images']['from']=getImageStreamTag(fromImageStreamTag)?.image?.metadata?.name

        if (bc.spec?.source?.images != null){
            for (Map image:bc.spec.source?.images){
                Map from = image.from
                record['images']["${from.namespace}/${from.kind}/${from.name}"]=getImageStreamTag(from)?.image?.metadata?.name
            }
        }

        String checksum='sha256:'+calculateChecksum(groovy.json.JsonOutput.toJson(record), 'SHA-256')
        //println record
        return checksum
    }

    private int pickNextItemsToBuild(List processed, List pending, List queue){
        int newItems=0
        List snapshot=[]
        snapshot.addAll(pending)

        def iterator = pending.iterator()

        while (iterator.hasNext()){
            Map bc = iterator.next()
            boolean picked=false

            //println "Checking ${key(bc)} spec.strategy"

            if ('ImageStreamTag'.equalsIgnoreCase(bc.spec?.strategy?.dockerStrategy?.from?.kind)){
                if (isNextToBuild(bc.spec?.strategy?.dockerStrategy?.from, snapshot, processed)){
                    picked = true
                }
            }else if ('ImageStreamTag'.equalsIgnoreCase(bc.spec?.strategy?.sourceStrategy?.from?.kind)){
                if (isNextToBuild(bc.spec?.strategy?.sourceStrategy?.from, snapshot, processed)){
                    picked = true
                }
            }else{
                throw new RuntimeException("I don't know how to handle this type of build! ${key(bc)}  -  :`(")
            }

            //Check for source of Chained Builds
            if (picked && bc.spec?.source?.images != null){
                //println "Checking ${key(bc)} spec.source.images"
                for (Map image:bc.spec.source?.images){
                    if (!isNextToBuild(image.from, snapshot, processed)){
                        picked=false
                        break;
                    }
                }
            }

            if (picked){
                newItems++
                queue.add(bc)
                iterator.remove()
            }
        }

        return newItems
    }

    public void build(){
        List pending=[]
        List processing=[]
        List processed=[]
        Map indexByKey=[:]
        
        //[buildConfig:null, state:'new', buildName:null, builds:0, dependsOn:[:], output:[from:[kind:''], to:['kind':'']] ]
        Map items = [:] //Indexed by key

        println 'Building ...'
        config.app.templates.build.each { template ->
            template.objects.each { object ->
                indexByKey["${key(object)}"] = object
                if ('BuildConfig'.equalsIgnoreCase(object.kind)){
                    pending.add(object)
                }
            }
        }

        while (pending.size()>0){

            def iterator = pending.iterator()
            boolean hasPickedNewOne=false

            if (pickNextItemsToBuild(processed, pending, processing) > 0){
                hasPickedNewOne=true
            }

            if (processing.size() == 0) {
                throw new RuntimeException("Oh oh! I've failed to predict the next build(s) :`(")
            }

            //This means that it hasn't found a new item to process.
            //and it is stuck waiting for others build to complete
            if (!hasPickedNewOne){
                println 'Waiting 5s'
                Thread.sleep(5 * 1000);
            }

            //}
            Map entryCount=[:]
            while (processing.size()>0){
                def iterator2 = processing.iterator()
                while (iterator2.hasNext()){
                    boolean skipThisOne=false
                    Map original = iterator2.next()
                    Map bc = toJson(groovy.json.JsonOutput.toJson(original))
                    println "Processing ${key(bc)}"

                    Map outputTo = bc.spec.output.to
                    int currentEntryCount = entryCount[key(bc)] = (entryCount[key(bc)]?:0) + 1

                    if (currentEntryCount > 10){
                        //backoff
                        pending.add(original)
                        iterator2.remove()
                    }

                    List relatedBuilds=getLatestRelatedBuilds(bc)
                    if (!allBuildsSuccessful(relatedBuilds)){
                        boolean backoff=false
                        //if the item is waiting for a build of a BuildConfig int the processing queue, backoff and return item to the pending queue
                        for (Map build:relatedBuilds){
                            if (isBuidActive(build)){
                                for (Map bc2:processing){
                                    if ("${key(bc2)}" == "${build.status.config.kind}/${build.status.config.name}"){
                                        backoff=true
                                    }
                                }
                            }else if (!isBuidSuccessful(build)){
                                for (Map bc2:processed){
                                    if ("${key(bc2)}" == "${build.status.config.kind}/${build.status.config.name}"){
                                        processed.remove(bc2)
                                        pending.add(bc2)
                                        break //end for loop
                                    }
                                }
                                //new to re-queue and try rebuilding it
                                backoff=true
                            }
                        }

                        //backoff
                        if (backoff){
                            pending.add(original)
                            iterator2.remove()
                        }

                        //skip to next item
                        continue
                    }
                    
                    List imageStreamTagReferences = getBuildConfigFromImageStreamTagReferences(bc)
                    List imageStreamTags = getImageStreamTags(imageStreamTagReferences)

                    if (imageStreamTagReferences.size() != imageStreamTags.size()){
                        throw new RuntimeException("One more required images are missing!")
                    }

                    String buildHash=calculateBuildHash(bc)

                    if (getVerboseLevel() >= 3) println "Build hash = ${buildHash}"

                    List images=getImageStreamTagByBuildHash(outputTo, buildHash)
                    addBuildConfigEnv(bc, [name:'_BUILD_HASH', value:buildHash])
                    Map ocApplyRet=ocApply([bc], ['-n', config.app.build.namespace])

                    if (images.size() == 0){
                        //Start New Build
                        //TODO: is nuild already in progress?
                        println "Starting New Build for ${key(bc)}"
                        println oc(['start-build', bc.metadata.name, '-n', 'csnr-devops-lab-tools'])
                    }else if (images.size() == 1){
                        println "Reusing exiting image (${images[0].name}) for ${key(bc)}"
                        Map outputImageStreagTag = getImageStreamTag(outputTo)
                        if (outputImageStreagTag==null || !images[0].name.equalsIgnoreCase(outputImageStreagTag.image.metadata.name)){
                            oc(['tag', '--source=imagestreamimage', "${outputTo.namespace?:config.app.build.namespace}/${outputTo.name.split(':')[0]}@${images[0].name}", "${outputTo.namespace?:config.app.build.namespace}/${outputTo.name}"])
                        }
                        //Reuse Image from a previous build
                    }else{
                        //hell broke loose!
                        throw new RuntimeException("Hell broke loose! ")
                    }
                    if (!skipThisOne){
                        processed.add(original)
                        iterator2.remove()
                    }
                } //end while (processing queue)

                //There are items left in the queue
                if (processing.size()>0){
                    println 'Waiting 4s'
                    Thread.sleep(4 * 1000);
                }
            } //end while (delay, retry)

        } // end while
    } //end build
}