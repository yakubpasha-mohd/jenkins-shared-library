def call(Map config = [:]) {

    if (!config.service || !config.registry || !config.tag) {
        error "Missing required config: service/registry/tag"
    }

    def image = "${config.registry}/${config.service}:${config.tag}"

    echo "🐳 Building image: ${image}"

    dir("services/${config.service}") {

        if (!fileExists("Dockerfile")) {
            error "Dockerfile not found for ${config.service}"
        }

        sh """
            docker build \
            --pull \
            -t ${image} \
            .
        """
    }

    echo "🚀 Pushing image: ${image}"
    sh "docker push ${image}"
}
