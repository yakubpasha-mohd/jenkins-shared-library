def call(Map config = [:]) {

    def service     = config.service
    def environment = config.environment
    def nexusHost   = config.nexusHost
    def tag         = config.tag

    if (!service || !environment || !nexusHost || !tag) {
        error "Missing required params for deployment"
    }

    def composeFile = (environment == 'dev') 
        ? "docker-compose.yml"
        : "docker-compose.${environment}.yml"

    if (!fileExists(composeFile)) {
        error "Compose file not found: ${composeFile}"
    }

    def image = "${nexusHost}:8082/${service}:${tag}"

    echo "⬇ Pulling image: ${image}"
    sh "docker pull ${image}"

    echo "🔄 Updating image for ${service}"
    sh """
        sed -i '/${service}/,/image:/ s|image:.*|image: ${image}|' ${composeFile}
    """

    if (environment == 'prod') {
        input message: "Approve deployment of ${service} to PROD?"
    }

    echo "🚀 Deploying ONLY ${service}"

    sh "docker compose -f ${composeFile} up -d --no-deps ${service}"

    echo "✅ Successfully deployed ${service}"
}
