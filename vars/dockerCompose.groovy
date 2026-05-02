def call(Map config = [:]) {

    def services    = config.services ?: []
    def environment = config.environment ?: 'dev'

    def composeCmd = "docker compose"

    if (environment == 'qa') {
        composeCmd += " -f docker-compose.qa.yml"
    } else if (environment == 'prod') {
        input message: "Approve deployment to PROD?"
        composeCmd += " -f docker-compose.prod.yml"
    }

    if (services.size() == 1) {
        // 🔥 single service → no dependencies
        def svc = services[0]
        echo "Deploying ONLY ${svc} (no dependencies)"
        sh "${composeCmd} up -d --no-deps ${svc}"
    } else {
        // 🔥 all services
        echo "Deploying ALL services"
        sh "${composeCmd} up -d"
    }
}
