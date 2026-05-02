def call(String svc) {
    stage("Checkmarx Scan ${svc}") {
        dir("services/${svc}") {
            step([
                $class: 'CxScanBuilder',
                projectName: svc,
                teamPath: "\\CxServer\\SP\\Company",
                credentialsId: 'checkmarx-creds',
                sastEnabled: true
            ])
        }
    }
}
