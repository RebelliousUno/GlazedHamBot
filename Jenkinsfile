
pipeline {
    agent any
    triggers {
        pollSCM 'H * * * *'
    }
    tools {
        jdk 'JDK 8'
    }
    stages {

        stage('clean') {
            steps {
                echo 'cleaning'
                withGradle {
                    bat 'gradlew.bat clean'
                }
            }
        }
        stage('build') {
            steps {
                echo 'building'
                bat 'gradlew.bat build'
            }
        }
        stage ('test') {
            steps {
                echo 'testing'
                bat 'gradlew.bat check'
            }
        }        
    }
     post {
        always {
            archiveArtifacts artifacts: 'build/libs/**/*.jar', fingerprint: true
        }
    }
}
