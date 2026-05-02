def call(Map config) {

    dir("services/${config.service}") {
        sh "docker build -t ${config.registry}/${config.service}:${config.tag} ."
    }

    withCredentials([usernamePassword(
        credentialsId: 'docker-cred',
        usernameVariable: 'DOCKER_USER',
        passwordVariable: 'DOCKER_PASS'
    )]) {

        sh """
            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
            docker push ${config.registry}/${config.service}:${config.tag}
        """
    }
}
