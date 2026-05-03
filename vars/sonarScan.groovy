def call(String svc) {

    echo "🔍 Running SonarQube scan for ${svc}"

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
