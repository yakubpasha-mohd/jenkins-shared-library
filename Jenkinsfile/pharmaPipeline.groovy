@Library('my-shared-lib') _

node('jenkins-slave') {

    def registry    = 'myptech'
    def branch      = params.BRANCH
    def repoUrl     = params.APPLICATION_REPO
    def environment = params.ENVIRONMENT

    def ALL_SERVICES = [
        'api-gateway','auth-service','drug-catalog-service',
        'notification-service','product-service','pharma-ui',
        'order-service','user-service'
    ]

    def services = (params.SERVICES == 'all') 
        ? ALL_SERVICES 
        : [params.SERVICES]

    echo "Resolved services: ${services}"

    /* ========================= */
    stage('Tools Setup') {
        def jdkHome     = tool name: 'openjdk-17', type: 'hudson.model.JDK'
        def mvnHome     = tool name: 'maven-3.9.6', type: 'hudson.tasks.Maven$MavenInstallation'
        def nodejsHome  = tool name: 'nodejs-20'
        def scannerHome = tool 'SonarScanner'

        env.PATH = "${jdkHome}/bin:${mvnHome}/bin:${nodejsHome}/bin:${scannerHome}/bin:${env.PATH}"
    }

    /* ========================= */
    stage('Checkout') {
        git branch: branch, url: repoUrl
    }

    /* ========================= */
    stage(params.SERVICES == 'all' ? 'Build All Services' : "Build ${params.SERVICES}") {
        services.each { svc ->
            buildService(svc)
        }
    }

    /* ========================= */
    stage(params.SERVICES == 'all' ? 'Test All Services' : "Test ${params.SERVICES}") {
        test(services: services, servicesDir: 'services')
    }

    /* ========================= */
    stage(params.SERVICES == 'all' ? 'Docker All Services' : "Docker ${params.SERVICES}") {
        services.each { svc ->
            dockerBuildPush(
                service: svc,
                registry: registry,
                tag: env.APP_IMAGE_ID
            )
        }
    }

    /* ========================= */
   def deployStageName = (params.SERVICES == 'all') 
    ? "Deploy ${environment} (All Services)" 
    : "Deploy ${environment} (${params.SERVICES})"

stage(deployStageName) {

    dockerCompose(
        services: services,
        environment: environment
    )
}
}
