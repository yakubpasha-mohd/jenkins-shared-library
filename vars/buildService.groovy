def call(String svc) {
    stage("Build ${svc}") {
        dir("services/${svc}") {
            sh 'mvn clean package -DskipTests=false'
        }
    }
}
