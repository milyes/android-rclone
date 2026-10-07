    private val googleAuthService = com.example.data.service.GoogleAuthService(application)

    fun runRcloneSync(recording: AudioRecording, destinationFolder: String = "gdrive:/Z-CORE/Captures/") {
        viewModelScope.launch {
            val token = googleAuthService.getAccessToken()
            if (token != null) {
                syncRecordingToGoogleDrive(recording, token)
            } else {
                Toast.makeText(getApplication(), "Connexion Google Drive requise", Toast.LENGTH_SHORT).show()
                // You might trigger a UI state change here to ask the user to log in
            }
        }
    }
