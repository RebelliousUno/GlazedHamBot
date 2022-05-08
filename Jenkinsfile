
pipeline {
    agent any
    triggers {
        pollSCM 'H * * * *'
    }
    tools {
        jdk 'JDK 17'
    }
    stages {

        stage('clean') {
            steps {
                echo 'cleaning'
                withGradle {
                    sh './gradlew clean'
                }
            }
        }
        stage('build') {
            steps {
                echo 'building'
                sh './gradlew build'
            }
        }
        stage ('test') {
            steps {
                echo 'testing'
                sh './gradlew check'
            }
        }        
    }
     post {
        always {
            archiveArtifacts artifacts: 'build/libs/**/*.jar', fingerprint: true
        }
    }
}
