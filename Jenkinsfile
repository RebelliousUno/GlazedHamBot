
pipeline {
agent any
stages {
stage('checkout'){steps{ checkout scm }}
stage('clean'){steps{ echo 'cleaning'}}
stage('build'){steps {echo 'building'} }
stage ('deploy'){steps {echo 'deploying'}}
}

}
