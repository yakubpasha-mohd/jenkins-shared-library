def call(Map config = [:]) {

    def service   = config.service
    def tag       = config.tag
    def registry  = config.registry
    def nexusHost = config.nexusHost   // only host (no port)
    def repoName  = config.repo ?: "docker-hosted"

    if (!service || !tag || !registry || !nexusHost) {
        error "Missing required params for Nexus push"
    }

    def sourceImage = "${registry}/${service}:${tag}"
    def targetImage = "${nexusHost}:8082/${service}:${tag}"    
    echo "📦 Pushing ${service} to Nexus"

    sh """
        docker tag ${sourceImage} ${targetImage}
        docker push ${targetImage}
    """

    // 🔗 Docker registry reference
    echo "🐳 Image: ${targetImage}"

    // 🌐 Browser link (Nexus UI)
    def nexusUiUrl = "http://${nexusHost}:8081/#browse/browse:${repoName}"

    echo "🌐 Nexus UI: ${nexusUiUrl}"
}
