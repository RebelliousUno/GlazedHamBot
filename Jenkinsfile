
pipeline {
agent any
stages {

stage('clean'){steps{ echo 'cleaning'
sh 'gradlew clean'}}
stage('build'){steps {echo 'building'} }
stage ('deploy'){steps {echo 'deploying'}}
}

}
