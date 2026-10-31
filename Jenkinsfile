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
        // Versión de la app (app/build.gradle.kts): sube con cada build para que el APK nuevo
        // se instale como actualización. Los builds de main generan 1.0.N.
        VERSION_CODE = "${BUILD_NUMBER}"
        VERSION_NAME = "1.0.${BUILD_NUMBER}"
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
            when {
                allOf {
                    branch 'main'
                    // Mientras no exista la llave de release en Jenkins (docs/CI_SETUP.md, paso 3)
                    // se omite el APK firmado en lugar de marcar el build de main como fallido.
                    expression { hayLlaveDeRelease() }
                }
            }
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

        // CD: cada build de main con llave se publica como release de GitHub (tag vX.Y.N) con el
        // APK firmado, para descargarlo e instalarlo desde el teléfono.
        stage('Publicar en GitHub') {
            when {
                allOf {
                    branch 'main'
                    expression { hayLlaveDeRelease() }
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'github-token',
                        usernameVariable: 'GH_USUARIO', passwordVariable: 'GH_TOKEN')]) {
                    sh '''
                        set -eu
                        REPO=CiscoPaquitoTracer/Gestor_Hamburgueseria
                        APK="monchosos-$VERSION_NAME.apk"
                        cp app/build/outputs/apk/release/app-release.apk "$APK"
                        printf '{"tag_name": "v%s", "target_commitish": "%s", "name": "Monchosos %s", "make_latest": "true", "body": "APK firmado por Jenkins (build %s de main)."}' "$VERSION_NAME" "$GIT_COMMIT" "$VERSION_NAME" "$BUILD_NUMBER" > release-pedido.json
                        curl -fsS -X POST -H "Authorization: Bearer $GH_TOKEN" -H "Accept: application/vnd.github+json" --data @release-pedido.json -o release.json "https://api.github.com/repos/$REPO/releases"
                        ID=$(grep -m1 -o '"id": *[0-9]*' release.json | grep -o '[0-9]*$')
                        curl -fsS -X POST -H "Authorization: Bearer $GH_TOKEN" -H "Content-Type: application/vnd.android.package-archive" --data-binary @"$APK" -o /dev/null "https://uploads.github.com/repos/$REPO/releases/$ID/assets?name=$APK"
                        # Mismo APK con nombre fijo: la página de descarga (pagina/) siempre apunta a
                        # releases/latest/download/monchosos.apk.
                        curl -fsS -X POST -H "Authorization: Bearer $GH_TOKEN" -H "Content-Type: application/vnd.android.package-archive" --data-binary @"$APK" -o /dev/null "https://uploads.github.com/repos/$REPO/releases/$ID/assets?name=monchosos.apk"
                        echo "Publicado: https://github.com/$REPO/releases/tag/v$VERSION_NAME"
                    '''
                }
            }
        }
    }
}

/** true si la credencial de la llave de release existe en Jenkins; si no, lo avisa en el log. */
def hayLlaveDeRelease() {
    try {
        withCredentials([file(credentialsId: 'android-keystore', variable: 'KEYSTORE_FILE')]) { }
        return true
    } catch (err) {
        echo 'Sin la credencial android-keystore: se omite Build Release (ver docs/CI_SETUP.md, paso 3).'
        return false
    }
}
