def call(Map config = [:]) {

    def services    = config.services ?: []
    def environment = config.environment ?: 'dev'

    services.each { svc ->

        echo "Deploying ${svc} to ${environment}"

        if (environment == 'dev') {

            sh "docker compose up -d ${svc}"

        } else if (environment == 'qa') {

            sh "docker compose -f docker-compose.qa.yml up -d ${svc}"

        } else if (environment == 'prod') {

            input message: "Deploy ${svc} to PROD?"

            sh "docker compose -f docker-compose.prod.yml up -d ${svc}"
        }
    }
}
