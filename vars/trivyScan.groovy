def call(Map config) {
    stage("Trivy Scan ${config.service}") {
        sh """
            trivy image --exit-code 0 --severity HIGH,CRITICAL \
            ${config.registry}/${config.service}:${config.tag}
        """
    }
}
