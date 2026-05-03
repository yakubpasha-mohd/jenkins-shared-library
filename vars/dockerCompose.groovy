def call(Map config = [:]) {

    def services    = config.services ?: []
    def environment = config.environment
    def nexusHost   = config.nexusHost
    def tag         = config.tag

    if (!services || !environment || !nexusHost || !tag) {
        error "Missing required params for deployment"
    }

    // 🔹 Compose file resolution
    def composeFile = (environment == 'dev') 
        ? "docker-compose.yml"
        : "docker-compose.${environment}.yml"

    if (!fileExists(composeFile)) {
        error "Compose file not found: ${composeFile}"
    }

    echo "🚀 Using compose file: ${composeFile}"
    echo "🎯 Services to deploy: ${services}"

    services.each { svc ->

        // 🔹 Stage naming per service
        stage("Deploy ${environment} :: ${svc}") {

            def image = "${nexusHost}:8082/${svc}:${tag}"

            echo "⬇ Pulling image: ${image}"
            sh "docker pull ${image}"

            // 🔹 Update ONLY this service image
            echo "🔄 Updating image for ${svc}"

            sh """
                sed -i '/${svc}/,/image:/ s|image:.*|image: ${image}|' ${composeFile}
            """

            // 🔹 Optional approval for PROD
            if (environment == 'prod') {
                input message: "Approve deployment of ${svc} to PROD?"
            }

            echo "🚀 Deploying ONLY ${svc} (no dependencies)"

            // 🔹 CRITICAL FIX → --no-deps
            sh """
                docker compose -f ${composeFile} up -d --no-deps ${svc}
            """

            echo "✅ Successfully deployed ${svc}"
        }
    }
}
