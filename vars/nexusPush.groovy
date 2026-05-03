def call(Map config = [:]) {

    def service   = config.service
    def tag       = config.tag
    def registry  = config.registry
    def nexusUrl  = config.nexusUrl

    if (!service || !tag || !registry || !nexusUrl) {
        error "Missing required params for Nexus push"
    }

    def sourceImage = "${registry}/${service}:${tag}"
    def targetImage = "${nexusUrl}/${service}:${tag}"

    echo "📦 Pushing ${service} to Nexus"

    sh """
        docker tag ${sourceImage} ${targetImage}
        docker push ${targetImage}
    """

    echo "✅ Nexus push completed: ${targetImage}"
}
