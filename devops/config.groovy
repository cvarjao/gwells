app {
    name = 'gwells'
    version = 'snapshot'
    templates {
        build  = [
          //['file':'../openshift/postgresql.bc.json'],
          ['file':'../openshift/backend.bc.json']
        ]
    }
    git {
        workDir = ['git', 'rev-parse', '--show-toplevel'].execute().text.trim()
        uri = ['git', 'config', '--get', 'remote.origin.url'].execute().text.trim()
        commit = ['git', 'rev-parse', 'HEAD'].execute().text.trim()
        ref = 'refs/pull/697/head'
        changeId = '697'
    }
    build {
        name = 'pr-697'
        version = '697'
        prefix = 'gwells-'
        suffix = '-697'
        namespace = 'csnr-devops-lab-tools'
    }
    deploy {
        dev {}
        test {}
        prod {}
    }
}
environments {
    dev {
    }
}