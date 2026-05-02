def call(Map config) {

    stage("Deploy ${config.environment}") {

        dir("${env.WORKSPACE}") {   // ✅ ensure root directory

            for (svc in config.services) {
                sh """
                sed -i 's|${config.registry}/${svc}:latest|${config.registry}/${svc}:${config.tag}|g' docker-compose.${config.environment}.yml
                """
            }

            sh """
                docker-compose -f docker-compose.${config.environment}.yml down || true
                docker-compose -f docker-compose.${config.environment}.yml up -d
            """
        }
    }
}
