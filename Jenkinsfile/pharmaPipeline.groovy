@Library('my-shared-lib') _

properties([
    parameters([
        string(
            name: 'APPLICATION_REPO',
            defaultValue: 'https://github.com/yakubpasha-mohd/pharmaops.git'
        ),
        string(
            name: 'BRANCH',
            defaultValue: 'main'
        ),
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev','qa','staging','prod']
        ),
        choice(
            name: 'SERVICES',
            choices: ['all','api-gateway','auth-service','drug-catalog-service','notification-service','product-service','pharma-ui','order-service','user-service']
        )
    ])
])

node('jenkins-slave') {

    def registry    = 'myptech'
    def branch      = params.BRANCH
    def repoUrl     = params.APPLICATION_REPO
    def environment = params.ENVIRONMENT
    def ALL_SERVICES = ['api-gateway', 'auth-service']
    def services = (params.SERVICES == 'all') 
    ? ALL_SERVICES 
    : [params.SERVICES]

echo "Resolved services: ${services}"
    // Convert string → list
    def services = params.SERVICES.split(',').collect { it.trim() }
    
    /* ========================= */
    stage('Tools Setup') {
        def jdkHome     = tool name: 'openjdk-17', type: 'hudson.model.JDK'
        def mvnHome     = tool name: 'maven-3.9.6', type: 'hudson.tasks.Maven$MavenInstallation'
        def nodejsHome = tool name: 'nodejs-20'
        def scannerHome = tool 'SonarScanner'
        env.JAVA_HOME  = jdkHome
        env.MAVEN_HOME = mvnHome
        env.NODE_HOME  = nodejsHome

        env.PATH = "${jdkHome}/bin:${mvnHome}/bin:${nodejsHome}/bin:${scannerHome}/bin:${env.PATH}"

        sh '''
echo "JAVA_HOME=$JAVA_HOME"
echo "MAVEN_HOME=$MAVEN_HOME"
echo "NODE_HOME=$NODE_HOME"
echo "PATH=$PATH"
'''
    }

    /* ========================= */
    stage('Clean Workspace') {
        cleanWs()
    }

    /* ========================= */
    stage('Checkout & Versioning') {
        git branch: branch, url: repoUrl

        def version = sh(
            script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout",
            returnStdout: true
        ).trim()

        def commitId = sh(
            script: "git rev-parse --short HEAD",
            returnStdout: true
        ).trim()

        env.APP_IMAGE_ID = "${version}-${env.BUILD_NUMBER}-${commitId}"

        writeFile file: 'build-info.txt', text: """
APP_IMAGE_ID=${env.APP_IMAGE_ID}
BUILD_NUMBER=${env.BUILD_NUMBER}
GIT_COMMIT=${commitId}
BRANCH=${branch}
ENVIRONMENT=${environment}
"""

        archiveArtifacts artifacts: 'build-info.txt'
    }

    /* ========================= */
    stage('Build Services') {
        for (svc in services) {
            echo "Building ${svc}"
            buildService(svc)
        }
    }

    /* ========================= */
    stage('Docker Build & Push') {
        for (svc in services) {
            echo "Docker build ${svc}"
            dockerBuildPush(
                service: svc,
                registry: registry,
                tag: env.APP_IMAGE_ID
            )
        }
    }

   
    /* ========================= */
    stage("Deploy to ${environment}") {
        deployCompose(
            services: services,
            registry: registry,
            tag: env.APP_IMAGE_ID,
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
