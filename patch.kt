    fun renameRecording(recording: AudioRecording, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            val updated = recording.copy(title = newTitle.trim())
            repository.updateRecording(updated)
            Toast.makeText(getApplication(), "Enregistrement renommé", Toast.LENGTH_SHORT).show()
        }
    }
