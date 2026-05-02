def call(Map config) {

    stage("Deploy ${config.environment}") {

        dir("${env.WORKSPACE}") {

            def composeFile = "docker-compose.${config.environment}.yml"

            if (!fileExists(composeFile)) {
                echo "⚠️ ${composeFile} not found, using default docker-compose.yml"
                composeFile = "docker-compose.yml"
            }

            echo "Using compose file: ${composeFile}"
            echo "Image tag: ${config.tag}"

            for (svc in config.services) {
                sh """
                sed -i 's|${config.registry}/${svc}:latest|${config.registry}/${svc}:${config.tag}|g' ${composeFile}
                """
            }

            sh """
                docker-compose -f ${composeFile} down || true
                docker-compose -f ${composeFile} up -d
            """
        }
    }
}
