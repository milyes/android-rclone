package com.example.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CommandMacro
import com.example.data.repository.TermuxRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MacroViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val macroDao = database.commandMacroDao()
    private val termuxRepository = TermuxRepository(application)

    val allMacros: StateFlow<List<CommandMacro>> = macroDao.getAllMacros()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveMacro(name: String, command: String, category: String = "TERMUX", description: String = "") {
        viewModelScope.launch {
            val macro = CommandMacro(
                name = name,
                commandText = command,
                category = category,
                description = description
            )
            macroDao.insertMacro(macro)
        }
    }

    fun triggerMacro(macro: CommandMacro) {
        termuxRepository.executeMacroIntent(macro)
    }

    fun toggleFavorite(macro: CommandMacro) {
        viewModelScope.launch {
            macroDao.toggleFavorite(macro.id, !macro.isFavorite)
        }
    }
}
