pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'archiva',
                    usernameVariable: 'ORG_GRADLE_PROJECT_openrs2RepoUsername',
                    passwordVariable: 'ORG_GRADLE_PROJECT_openrs2RepoPassword'
                )]) {
                    withGradle {
                        sh './gradlew --no-daemon clean build publish'
                    }
                }
            }
        }
    }

    post {
        always {
            junit '**/build/test-results/test/*.xml'
            jacoco(
                execPattern: '**/build/jacoco/test.exec',
                classPattern: '**/build/classes/*/main',
                sourcePattern: '**/src/main'
            )
        }
    }
}