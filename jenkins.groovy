pipeline {
    agent { label 'linux' }
    
    parameters {
        choice(name: 'ACTION', choices: ['DELETE_SERVICE', 'DELETE_TARGET_GROUP'], description: 'Select the action to perform')
        string(name: 'ARN_TO_DELETE', description: 'ARN of the service or target group to delete')
        string(name: 'AWS_ACCOUNT', description: 'AWS Account to deploy')
        string(name: 'AWS_REGION', defaultValue: 'us-east-1', description: 'AWS Default Region')
    }
    
    stages {
        stage('Execute Action') {
            steps {
                script {
                    def awsCredentials = [
                        [$class: 'StringBinding', credentialsId: "${params.AWS_ACCOUNT}-key", variable: 'AWS_ACCESS_KEY_ID'],
                        [$class: 'StringBinding', credentialsId: "${params.AWS_ACCOUNT}-key", variable: 'AWS_SECRET_ACCESS_KEY']
                    ]
                    
                    withEnv(["AWS_DEFAULT_REGION=${params.AWS_REGION}"]) {
                        withCredentials(awsCredentials) {
                            if (params.ACTION == 'DELETE_SERVICE') {
                                sh "aws ecs update-service --service ${params.ARN_TO_DELETE} --desired-count 0"
                                sh "sleep 20" // waiting for a couple of seconds
                                sh "aws ecs delete-service --service ${params.ARN_TO_DELETE}"
                            } 
                            
                            if (params.ACTION == 'DELETE_TARGET_GROUP') {
                                // Assuming that listener rules deletion is handled separately or there are none.
                                sh "aws elbv2 delete-target-group --target-group-arn ${params.ARN_TO_DELETE}"
                            }
                        }
                    }
                }
            }
        }
    }
}