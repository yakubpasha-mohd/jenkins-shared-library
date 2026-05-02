def call(Map config) {

    stage("Deploy ${config.environment}") {

        echo "Deploying with tag: ${config.tag}"

        dir("${env.WORKSPACE}") {

            sh "ls -l"

            if (!fileExists("docker-compose.${config.environment}.yml")) {
                error "Compose file missing!"
            }

            for (svc in config.services) {
                sh """
                sed -i 's|${config.registry}/${svc}:latest|${config.registry}/${svc}:${config.tag}|g' docker-compose.${config.environment}.yml
                """
            }

            sh """
                docker-compose -f docker-compose.${config.environment}.yml up -d
            """
        }
    }
}
