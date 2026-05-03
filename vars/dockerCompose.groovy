def call(Map config = [:]) {

    def services    = config.services ?: []
    def environment = config.environment
    def nexusHost   = config.nexusHost
    def tag         = config.tag

    if (!services || !environment || !nexusHost || !tag) {
        error "Missing required params for deployment"
    }

    def composeFile = "docker-compose.yml"

    if (environment != 'dev') {
        composeFile = "docker-compose.${environment}.yml"
    }

    if (!fileExists(composeFile)) {
        error "Compose file not found: ${composeFile}"
    }

    echo "🚀 Deploying using ${composeFile}"

    services.each { svc ->

        def image = "${nexusHost}:8082/${svc}:${tag}"

        echo "⬇ Pulling image: ${image}"
        sh "docker pull ${image}"

        echo "🔄 Updating compose for ${svc}"

        sh """
            sed -i 's|image: .*${svc}:.*|image: ${image}|g' ${composeFile}
        """

        echo "🚀 Deploying ${svc}"

        if (environment == 'prod') {
            input "Approve deployment of ${svc} to PROD?"
        }

        sh "docker compose -f ${composeFile} up -d ${svc}"
    }
}
