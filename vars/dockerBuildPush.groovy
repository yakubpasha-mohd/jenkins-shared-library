def call(Map config) {

    def image = "${config.registry}/${config.service}:${config.tag}"

    echo "Building image: ${image}"

    dir("services/${config.service}") {

        sh """
            docker build \
            --pull \
            -t ${image} \
            .
        """
    }

    echo "Pushing image: ${image}"

    sh "docker push ${image}"
}
