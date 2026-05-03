def call(Map config = [:]) {

    def timeoutMinutes = config.timeout ?: 5
    def abortPipeline  = config.abortPipeline != false  // default = true

    echo "⏳ Waiting for SonarQube Quality Gate..."

    timeout(time: timeoutMinutes, unit: 'MINUTES') {

        def qg = waitForQualityGate()

        echo "📊 Quality Gate Status: ${qg.status}"

        if (qg.status != 'OK') {

            def message = "❌ Quality Gate failed: ${qg.status}"

            if (abortPipeline) {
                error(message)
            } else {
                echo "⚠️ ${message} (continuing pipeline)"
            }
        } else {
            echo "✅ Quality Gate PASSED"
        }
    }
}
