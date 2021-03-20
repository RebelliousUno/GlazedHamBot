
pipeline {
agent any
stages {

stage('clean'){steps{ echo 'cleaning'
bat 'gradlew.bat clean'}}
stage('build'){steps {echo 'building'} }
stage ('deploy'){steps {echo 'deploying'}}
}

}
