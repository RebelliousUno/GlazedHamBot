
pipeline {
    agent any
    triggers {
        cron 'H * * * *'
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
            }
        }
        stage ('deploy') {
            steps {
                echo 'deploying'
            }
        }
    }
}
