@Library('pharma-shared-lib') _

properties([
    parameters([
        string(name: 'APPLICATION_REPO',
               defaultValue: 'https://github.com/yakubpasha-mohd/pharmaops.git'),
        string(name: 'BRANCH',
               defaultValue: 'main'),
        choice(name: 'ENVIRONMENT',
               choices: ['dev','qa','staging','prod'])
    ])
])

node {

    def services    = ['api-gateway', 'auth-service']
    def registry    = 'your-dockerhub-user'
    def branch      = params.BRANCH
    def repoUrl     = params.APPLICATION_REPO
    def environment = params.ENVIRONMENT

    stage('Tools Setup') {
        def jdkHome     = tool name: 'JDK17', type: 'hudson.model.JDK'
        def mvnHome     = tool name: 'Maven3', type: 'hudson.tasks.Maven$MavenInstallation'
        def scannerHome = tool 'SonarScanner'

        env.JAVA_HOME  = jdkHome
        env.MAVEN_HOME = mvnHome
        env.PATH       = "${jdkHome}/bin:${mvnHome}/bin:${scannerHome}/bin:${env.PATH}"
    }

    stage('Clean Workspace') {
        cleanWs()
    }

    stage('Checkout') {
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

    stage('Build, Scan & Quality') {
        for (svc in services) {
            buildService(svc)
            nexusScan(svc)
            checkmarxScan(svc)
            sonarScan(svc)
        }
    }

    stage('Docker Build, Push & Trivy') {
        for (svc in services) {
            dockerBuildPush(
                service: svc,
                registry: registry,
                tag: env.APP_IMAGE_ID
            )

            trivyScan(
                service: svc,
                registry: registry,
                tag: env.APP_IMAGE_ID
            )
        }
    }

    stage("Deploy to ${environment}") {
        deployCompose(
            services: services,
            registry: registry,
            tag: env.APP_IMAGE_ID,
            environment: environment
        )
    }

    stage('Cleanup') {
        sh '''
            docker logout || true
            docker system prune -f || true
        '''
    }
}
