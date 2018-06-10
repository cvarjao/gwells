package ca.bc.gov.devops

class OpenShiftHelper{
    public static int getVerboseLevel(){
        return 1
    }
    public static String key(Map object){
        return "${object.kind}/${object.metadata.name}"
    }

    public static Map _exec(List args){
       return _exec(args.execute())
    }

    public static Map _exec(java.lang.Process proc){
        return _exec(proc, new StringBuffer(), new StringBuffer())
    }

    public static Map _exec(java.lang.Process proc, StringBuffer stdout, StringBuffer stderr){
        proc.waitForProcessOutput(stdout, stderr)
        int exitValue= proc.exitValue()
        Map ret = ['out': stdout, 'err': stderr, 'status':exitValue]
        return ret
    }

    public static Map oc(List args){
        return oc(args, new StringBuffer(), new StringBuffer())
    }
    
    public static Map oc(List args, StringBuffer stdout){
        return oc(args, stdout, stdout)
    }

    public static Map oc(List args, StringBuffer stdout, StringBuffer stderr){
        List _args = ['oc'];
        _args.addAll(args)

        if (getVerboseLevel() >= 4) println _args.join(" ")

        def proc = _args.execute()

        proc.waitForProcessOutput(stdout, stderr)


        int exitValue= proc.exitValue()

        
        Map ret = ['out': stdout, 'err': stderr, 'status':exitValue, 'cmd':_args]
        if (exitValue != 0){
            throw new RuntimeException("oc returned an error code: ${ret}")
        }
        return ret
    }
    public static Map ocGet(List args){
        List _args = ['get'] + args + ['-o', 'json']
        Map ret=oc(_args)
        if (ret.out!=null && ret.out.length() > 0){
            return toJson(ret.out)
        }
        return null
    }
    public static Map ocApply(List items, List args){
        List _args = ['oc', 'apply', '-f', '-'] + args 
        String json=new groovy.json.JsonBuilder(['kind':'List', 'apiVersion':'v1', 'items':items]).toPrettyString()

        def proc = _args.execute()
        OutputStream out = proc.getOutputStream();
        out.write(json.getBytes());
        out.flush();
        out.close();

        Map ret= _exec(proc)

        return ret
    }
    public static def toJson(StringBuffer json){
        return toJson(json.toString())
    }
    public static def toJson(String jsonAsText){
        return new groovy.json.JsonSlurper().parseText(jsonAsText)
    }
    /*
    same output as:
       echo 'test content' | git hash-object --stdin --no-filters
       printf "test content\n" | git hash-object --stdin --no-filters
    */
    public static String gitHashAsBlobObject(String content) {
        calculateChecksum("blob ${content.length() + 1 }\0${content}\n", 'SHA1')
    }

    public static String calculateChecksum(String content, String type) {
        def digest = java.security.MessageDigest.getInstance(type)
        def buffer = content.getBytes(java.nio.charset.StandardCharsets.UTF_8)

        digest.update(buffer, 0, buffer.length)

        return digest.digest().encodeHex();
    }

    public static void addBuildConfigEnv(Map buildConfig, Map env){
        Map strategyConfig=buildConfig.spec.strategy.sourceStrategy?:buildConfig.spec.strategy.dockerStrategy
        strategyConfig.env=strategyConfig.env?:[]

        strategyConfig.env.add(env)
    }

}