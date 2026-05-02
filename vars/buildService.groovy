def call(String svc) {

    stage("Build ${svc}") {

        if (svc == 'all') {
            error "Invalid service name 'all'"
        }

        dir("services/${svc}") {

            sh '''
                echo "Building service: $(pwd)"

                if [ -f pom.xml ]; then
                    echo "Detected Maven project"
                    mvn clean package -DskipTests=false -U

                elif [ -f package.json ]; then
                    echo "Detected Node project"
                    npm ci
                    npm run build || echo "No build script"

                else
                    echo "Unknown project type"
                    exit 1
                fi
            '''
        }
    }
}
