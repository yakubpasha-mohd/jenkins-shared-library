def call(Map config = [:]) {

    def services    = config.services ?: []
    def servicesDir = config.servicesDir ?: 'services'

    def tests = [:]

    services.each { svc ->

        tests[svc] = {
            dir("${servicesDir}/${svc}") {

                sh '''
                    echo "Running tests in $(pwd)"

                    if [ -f pom.xml ]; then
                        mvn test || true

                    elif [ -f package.json ]; then
                        npm ci
                        npm test -- --watchAll=false --runInBand --silent --forceExit || true
                    fi
                '''

                if (fileExists('pom.xml') && fileExists('target/surefire-reports')) {
                    junit allowEmptyResults: true,
                          testResults: 'target/surefire-reports/*.xml'
                }
            }
        }
    }

    parallel tests
}
