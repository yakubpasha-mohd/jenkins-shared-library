def call(Map config) {

    stage("Docker Build ${config.service}") {
        dir("services/${config.service}") {
            sh "docker build -t ${config.registry}/${config.service}:${config.tag} ."
        }
    }

    stage("Docker Push ${config.service}") {
        withCredentials([usernamePassword(
            credentialsId: 'docker-creds',
            usernameVariable: 'DOCKER_USER',
            passwordVariable: 'DOCKER_PASS'
        )]) {
            sh """
                echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                docker push ${config.registry}/${config.service}:${config.tag}
            """
        }
    }
}
