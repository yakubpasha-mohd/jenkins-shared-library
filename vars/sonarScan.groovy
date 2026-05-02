def call(String svc) {
    stage("Sonar Scan ${svc}") {
        dir("services/${svc}") {
            withSonarQubeEnv('SonarQube') {
                sh """
                    mvn sonar:sonar \
                    -Dsonar.projectKey=${svc} \
                    -Dsonar.projectName=${svc}
                """
            }
        }
    }
}
