def call(Map config = [:]) {

    def service  = config.service
    def registry = config.registry
    def tag      = config.tag

    if (!service || !registry || !tag) {
        error "Missing required params: service/registry/tag"
    }

    def image  = "${registry}/${service}:${tag}"
    def report = "trivy-${service}.html"

    echo "🔍 Trivy scanning image: ${image}"

    sh """
        docker run --rm \
          -v /var/run/docker.sock:/var/run/docker.sock \
          -v \$WORKSPACE:/workspace \
          aquasec/trivy:latest image \
          --severity HIGH,CRITICAL \
          --format template \
          --template "@/contrib/html.tpl" \
          --output /workspace/${report} \
          ${image}
    """

    archiveArtifacts artifacts: report

    echo "🌐 Trivy Report: ${env.BUILD_URL}artifact/${report}"
}
