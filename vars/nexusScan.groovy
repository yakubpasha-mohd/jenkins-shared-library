def call(String svc) {
    stage("Nexus IQ Scan ${svc}") {
        dir("services/${svc}") {
            nexusPolicyEvaluation(
                iqApplication: svc,
                iqStage: 'build',
                jobCredentialsId: 'nexus-iq-creds',
                iqScanPatterns: [[scanPattern: '**/target/*.jar']]
            )
        }
    }
}
