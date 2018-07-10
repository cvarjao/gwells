app {
    name = 'gwells'
    version = 'snapshot'
    templates {
        build  = [
          ['file':'../openshift/backend.bc.json']
        ]
        deployment = [
            ['file':'../openshift/backend.dc.json']
        ]
    }

    git {
        workDir = ['git', 'rev-parse', '--show-toplevel'].execute().text.trim()
        uri = ['git', 'config', '--get', 'remote.origin.url'].execute().text.trim()
        commit = ['git', 'rev-parse', 'HEAD'].execute().text.trim()
        ref = ['bash','-c', 'git config branch.`git name-rev --name-only HEAD`.merge'].execute().text.trim()
        changeId = '697'
    }

    build {
        name = 'pr-697'
        version = '697'
        prefix = 'gwells-'
        suffix = '-697'
        namespace = 'csnr-devops-lab-tools'
        timeoutInSeconds = 60*20 // 20 minutes
        templates = [
                ['file':'../openshift/backend.bc.json']
        ]
    }

    deployment {
        name = 'pr-697'
        version = '697'
        prefix = 'gwells-'
        suffix = '-697'
        namespace = 'csnr-devops-lab-deploy'
        timeoutInSeconds = 60*20 // 20 minutes
        templates = [
                ['file':'../openshift/backend.dc.json']
        ]
    }
}

environments {
    dev {
        app{
            deployment {
                namespace = 'csnr-devops-lab-deploy'
            }
        }
    }
}