@Library('my-shared-lib') _
properties([
    parameters([

        string(
            name: 'APPLICATION_REPO',
            defaultValue: 'https://github.com/yakubpasha-mohd/pharmaops.git',
            description: 'Git repository URL'
        ),

        string(
            name: 'BRANCH',
            defaultValue: 'main',
            description: 'Git branch'
        ),

        choice(
            name: 'ENVIRONMENT',
            choices: ['dev','test','qa','prod']
        ),

        choice(
            name: 'SERVICES',
            choices: [
                'all',
                'api-gateway',
                'auth-service',
                'drug-catalog-service',
                'notification-service',
                'product-service',
                'pharma-ui',
                'order-service',
                'user-service'
            ]
        )
    ])
])
node 
   {

    /* ========================= */       
    def registry    = 'myptech'
    def branch      = params.BRANCH
    def repoUrl     = params.APPLICATION_REPO
    def environment = params.ENVIRONMENT
    def selectedService = params.SERVICES
    def NEXUS_URL = "100.50.84.49:8082"
    def ALL_SERVICES = [
    'api-gateway','auth-service','drug-catalog-service',
    'notification-service','product-service','pharma-ui',
    'order-service','user-service'
]

def services = (selectedService == 'all')
    ? ALL_SERVICES
    : [selectedService]

    echo "ENV = ${environment}"
    echo "SERVICES = ${services}"

    /* ========================= */
    stage('Setup Tools') {

        def jdkHome     = tool name: 'openjdk-17', type: 'hudson.model.JDK'
        def mvnHome     = tool name: 'maven-3.9.6', type: 'hudson.tasks.Maven$MavenInstallation'
        def nodejsHome  = tool name: 'nodejs-20'
        def scannerHome = tool name: 'SonarQube'

        env.JAVA_HOME  = jdkHome
        env.MAVEN_HOME = mvnHome
        env.NODE_HOME  = nodejsHome

        env.PATH = "${jdkHome}/bin:${mvnHome}/bin:${nodejsHome}/bin:${scannerHome}/bin:${env.PATH}"

        sh '''
            echo "JAVA_HOME=$JAVA_HOME"
            echo "MAVEN_HOME=$MAVEN_HOME"
            echo "NODE_HOME=$NODE_HOME"
        '''
    }

    /* ========================= */
    stage('Checkout & Versioning') {

        cleanWs()
        git branch: branch, url: repoUrl

        script {
            def version = sh(
                script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout",
                returnStdout: true
            ).trim()

            def commitId = sh(
                script: "git rev-parse --short HEAD",
                returnStdout: true
            ).trim()

            env.APP_IMAGE_ID = "${version}-${env.BUILD_NUMBER}-${commitId}"

            echo "🏷️ APP_IMAGE_ID = ${env.APP_IMAGE_ID}"
        }
    }

    /* ========================= */
    stage(params.SERVICES == 'all' 
        ? 'Build All Services' 
        : "Build ${params.SERVICES}") {

        services.each { svc ->
            echo "🔨 Building ${svc}"
            buildService(svc)
        }
    }

    /* ========================= */
    stage(params.SERVICES == 'all' 
        ? 'Test All Services' 
        : "Test ${params.SERVICES}") {

        test(
            services: services,
            servicesDir: 'services'
        )
    }
    /* ========================== */
    stage(params.SERVICES == 'all' 
    ? 'Sonar Scan (All Services)' 
    : "Sonar Scan (${params.SERVICES})") {

    services.each { svc ->

        echo "🔍 Sonar scanning ${svc}"

        sonarScan(svc)
    }
}
    /* ========================= */
      stage('Quality Gate') {
    qualityGate(
        timeout: 5,
        abortPipeline: true
    )
}
    /* ========================= */
    stage(params.SERVICES == 'all' 
        ? 'Docker All Services' 
        : "Docker ${params.SERVICES}") {

        if (!env.APP_IMAGE_ID) {
            error "APP_IMAGE_ID missing before Docker stage"
        }

        withCredentials([usernamePassword(
            credentialsId: 'docker-cred',
            usernameVariable: 'DOCKER_USER',
            passwordVariable: 'DOCKER_PASS'
        )]) {

            sh '''
                echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
            '''

            services.each { svc ->

                echo "🐳 Building Docker image for ${svc}"

                dockerBuildPush(
                    service: svc,
                    registry: registry,
                    tag: env.APP_IMAGE_ID
                )
            }
        }
    }
    /* ------------------------- */
       stage(params.SERVICES == 'all' 
    ? 'Push to Nexus (All Services)' 
    : "Push to Nexus (${params.SERVICES})") {

    withCredentials([usernamePassword(
        credentialsId: 'nexus-cred',
        usernameVariable: 'NEXUS_USER',
        passwordVariable: 'NEXUS_PASS'
    )]) {

        sh """
            echo \$NEXUS_PASS | docker login ${NEXUS_URL} -u \$NEXUS_USER --password-stdin
        """

        services.each { svc ->

            echo "📦 Nexus push for ${svc}"

            nexusPush(
                service: svc,
                tag: env.APP_IMAGE_ID,
                registry: registry,
                nexusUrl: "${NEXUS_URL}/repository/docker-hosted"
            )
        }
    }
}
    /* ------------------------- */
       stage(params.SERVICES == 'all' 
    ? 'Trivy Scan (All Services)' 
    : "Trivy Scan (${params.SERVICES})") {

    services.each { svc ->

        echo "🔍 Trivy scan for ${svc}"

        trivyScan(
            service: svc,
            registry: registry,
            tag: env.APP_IMAGE_ID
        )
    }
}
    /* ========================= */
    stage(params.SERVICES == 'all' 
        ? "Deploy ${environment} (All Services)" 
        : "Deploy ${environment} (${params.SERVICES})") {

        dockerCompose(
            services: services,
            environment: environment
        )
    }

    /* ========================= */
    stage('Cleanup') {
        sh '''
            docker logout || true
            docker system prune -f || true
        '''
    }
}
