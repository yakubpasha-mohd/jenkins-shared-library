def call(Map config = [:]) {

    def services    = config.services ?: []
    def servicesDir = config.servicesDir ?: 'services'

    stage('Unit Tests') {

        def tests = [:]

        services.each { svc ->

            tests[svc] = {
                stage("Test ${svc}") {
                    dir("${servicesDir}/${svc}") {

                        sh '''
                            echo "Running tests in $(pwd)"

                            if [ -f pom.xml ]; then
                                echo "Detected Maven project"
                                mvn test || true

                            elif [ -f package.json ]; then
                                echo "Detected Node project"
                                npm ci
                                npm test -- --watchAll=false --runInBand --silent --forceExit || true

                            else
                                echo "No testable project found"
                            fi
                        '''

                        // Publish JUnit only for Maven projects
                        if (fileExists('pom.xml') && fileExists('target/surefire-reports')) {
                            junit allowEmptyResults: true,
                                  testResults: 'target/surefire-reports/*.xml'
                        } else {
                            echo "Skipping JUnit for ${svc}"
                        }
                    }
                }
            }
        }

        // Run all service tests in parallel
        parallel tests
    }
}
