// CI/CD de Monchosos en Jenkins (Multibranch Pipeline).
// Cada push y cada PR (incluidos los de Dependabot) pasa por Lint, Unit Tests y Build Debug
// dentro del contenedor de ci/Dockerfile. Solo la rama main genera el release firmado.
//
// Las pruebas de la base de datos (Supabase) y el análisis de seguridad siguen en GitHub Actions.
// Guía de instalación: docs/CI_SETUP.md
pipeline {
    agent {
        dockerfile {
            filename 'Dockerfile'
            dir 'ci'
        }
    }

    environment {
        GRADLE_USER_HOME = "${WORKSPACE}/.gradle-home"
        GRADLE_OPTS = '-Dorg.gradle.daemon=false'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '15'))
        disableConcurrentBuilds()
        timestamps()
    }

    stages {
        stage('Lint') {
            steps {
                sh './gradlew --console=plain lintDebug'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'app/build/reports/lint-results-debug.html', allowEmptyArchive: true
                }
            }
        }

        stage('Unit Tests') {
            steps {
                sh './gradlew --console=plain testDebugUnitTest'
            }
            post {
                always {
                    junit testResults: 'app/build/test-results/testDebugUnitTest/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Build Debug') {
            steps {
                sh './gradlew --console=plain assembleDebug'
                archiveArtifacts artifacts: 'app/build/outputs/apk/debug/*.apk', fingerprint: true
            }
        }

        stage('Build Release') {
            when { branch 'main' }
            steps {
                withCredentials([
                    file(credentialsId: 'android-keystore', variable: 'KEYSTORE_FILE'),
                    string(credentialsId: 'keystore-password', variable: 'KEYSTORE_PASSWORD'),
                    string(credentialsId: 'key-alias', variable: 'KEY_ALIAS'),
                    string(credentialsId: 'key-password', variable: 'KEY_PASSWORD'),
                    // Configuración de la app (sin esto el release compila pero no puede iniciar sesión).
                    string(credentialsId: 'google-web-client-id', variable: 'GOOGLE_WEB_CLIENT_ID'),
                    string(credentialsId: 'supabase-url', variable: 'SUPABASE_URL'),
                    string(credentialsId: 'supabase-publishable-key', variable: 'SUPABASE_PUBLISHABLE_KEY'),
                ]) {
                    sh './gradlew --console=plain bundleRelease assembleRelease'
                }
                archiveArtifacts artifacts: 'app/build/outputs/bundle/release/*.aab, app/build/outputs/apk/release/*.apk',
                    fingerprint: true
            }
        }
    }
}
