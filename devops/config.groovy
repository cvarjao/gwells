package ca.bc.gov.devops.scripts

app {
    name = 'gwells'
    version = 'snapshot'
    environments = ['dev', 'dev']

    build {
        name = "pr-${CHANGE_ID}"
        prefix = "${app.name}-"
        suffix = "-${CHANGE_ID}"
        timeoutInSeconds = 60*20 // 20 minutes
        templates = [
                ['file':'../openshift/postgresql.bc.json'],
                ['file':'../openshift/backend.bc.json']
        ]
    }

    deployment {
        name = "pr-${CHANGE_ID}"
        prefix = "${app.name}-"
        suffix = "-${CHANGE_ID}"
        timeoutInSeconds = 60*20 // 20 minutes
        templates = [
                ['file':'../openshift/postgresql.dc.json',
                'params':[
                    'DATABASE_SERVICE_NAME':"gwells-pgsql${app.deployment.suffix}",
                    'IMAGE_STREAM_NAMESPACE':'',
                    'IMAGE_STREAM_NAME':"gwells-postgresql${app.deployment.suffix}",
                    'IMAGE_STREAM_VERSION':"${app.deployment.name}",
                    'POSTGRESQL_DATABASE':'gwells',
                    'VOLUME_CAPACITY':"${VOLUME_CAPACITY?:'1Gi'}",
                    'HOST':"${HOST?:''}"
                    ]
                ],
                ['file':'../openshift/backend.dc.json']
        ]
    }

    // Add environment-specific overrides in this section
    environments {
        dev {
            build {
                namespace = 'csnr-devops-lab-tools'
            }
            deployment {
                namespace = 'csnr-devops-lab-deploy'
            }
        }
    }
}