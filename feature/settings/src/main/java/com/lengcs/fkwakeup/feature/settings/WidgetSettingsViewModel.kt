package com.lengcs.fkwakeup.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lengcs.fkwakeup.widget.glance.WidgetConfig
import com.lengcs.fkwakeup.widget.glance.WidgetInstance
import com.lengcs.fkwakeup.widget.glance.WidgetRefreshScheduler
import com.lengcs.fkwakeup.widget.glance.WidgetSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetSettingsViewModel @Inject constructor(
    private val repository: WidgetSettingsRepository,
    @ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {
    private val _instances = MutableStateFlow<List<WidgetInstance>>(emptyList())
    val instances: StateFlow<List<WidgetInstance>> = _instances.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch { _instances.value = repository.instances() }
    fun save(appWidgetId: Int, config: WidgetConfig) = viewModelScope.launch {
        repository.save(appWidgetId, config)
        WidgetRefreshScheduler.refreshNow(appContext)
        refresh()
    }
}
