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
                                try {
                                    sh "aws ecs update-service --service ${params.ARN_TO_DELETE} --desired-count 0"
                                    sh "sleep 20" // waiting for a couple of seconds
                                    sh "aws ecs delete-service --service ${params.ARN_TO_DELETE}"
                                    echo "Service ${params.ARN_TO_DELETE} deleted successfully."
                                } catch (Exception e) {
                                    echo "Error deleting service: ${e.getMessage()}"
                                }
                            } 
                            
                            if (params.ACTION == 'DELETE_TARGET_GROUP') {
                                try {
                                    def listenerRules = sh(script: "aws elbv2 describe-rules --listener-arn ${params.ARN_TO_DELETE} --query 'Rules[].RuleArn' --output text", returnStdout: true).trim().split("\\s+")
                                    listenerRules.each { rule ->
                                        sh "aws elbv2 delete-rule --rule-arn ${rule}"
                                    }
                                    echo "Listener rules deleted successfully."
                                    
                                    sh "aws elbv2 delete-target-group --target-group-arn ${params.ARN_TO_DELETE}"
                                    echo "Target group ${params.ARN_TO_DELETE} deleted successfully."
                                } catch (Exception e) {
                                    echo "Error deleting target group and associated listener rules: ${e.getMessage()}"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

