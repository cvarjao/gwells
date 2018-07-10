app {
    name = 'gwells'
    version = 'snapshot'

    git {
        workDir = ['git', 'rev-parse', '--show-toplevel'].execute().text.trim()
        uri = ['git', 'config', '--get', 'remote.origin.url'].execute().text.trim()
        commit = ['git', 'rev-parse', 'HEAD'].execute().text.trim()
        ref = ['bash','-c', 'git config branch.`git name-rev --name-only HEAD`.merge'].execute().text.trim()
        changeId = '697'
    }

    build {
        name = "pr-${app.git.changeId}"
        prefix = "${app.name}-"
        suffix = "-${app.git.changeId}"
        namespace = 'csnr-devops-lab-tools'
        timeoutInSeconds = 60*20 // 20 minutes
        templates = [
                ['file':'../openshift/postgresql.bc.json'],
                ['file':'../openshift/backend.bc.json']
        ]
    }

    deployment {
        name = "pr-${app.git.changeId}"
        prefix = "${app.name}-"
        suffix = "-${app.git.changeId}"
        namespace = 'csnr-devops-lab-deploy'
        timeoutInSeconds = 60*20 // 20 minutes
        templates = [
                ['file':'../openshift/postgresql.dc.json'],
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