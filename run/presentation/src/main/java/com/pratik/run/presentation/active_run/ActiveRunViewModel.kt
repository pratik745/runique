package com.pratik.run.presentation.active_run

import androidx.lifecycle.viewModelScope
import com.pratik.core.domain.location.Location
import com.pratik.core.presentation.designsystem.base.BaseViewModel
import com.pratik.core.domain.run.Run
import com.pratik.core.domain.run.RunRepository
import com.pratik.core.presentation.ui.asUiText
import com.pratik.run.domain.LocationDataCalculator
import com.pratik.run.domain.RunningTracker
import com.pratik.run.presentation.active_run.service.ActiveRunService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import java.time.ZonedDateTime

class ActiveRunViewModel(
    private val runningTracker: RunningTracker,
    private val runRepository: RunRepository
): BaseViewModel<ActiveRunAction,ActiveRunState>() {

    private val eventChannel = Channel<ActiveRunEvents>()
    val events = eventChannel.receiveAsFlow()

    private val shouldTrack = state
        .map { it.shouldTrack }
        .stateIn(viewModelScope, SharingStarted.Lazily,state.value.shouldTrack)

    private val hasLocationPermission = MutableStateFlow(false)

    private val isTracking = combine(
        shouldTrack,
        hasLocationPermission
    ) { shouldTrack, hasLocationPermission ->
        shouldTrack && hasLocationPermission
    }.stateIn(viewModelScope, SharingStarted.Lazily,false)

    init {
        observeLocationPermission()
        observeTrackingState()
        observeRunningTracker()
        updateState { state ->
            state.copy(
                shouldTrack = ActiveRunService.isServiceActive && runningTracker.isTracking.value,
                hasStartedRunning = ActiveRunService.isServiceActive
            )
        }
    }

    override fun initState() = ActiveRunState()

    override fun onAction(action: ActiveRunAction) {
        when(action) {
            ActiveRunAction.OnBackClick -> {
                updateState { it.copy(shouldTrack = false) }
            }
            ActiveRunAction.OnFinishRunClick -> {
                updateState { it.copy(
                    isRunFinished = true,
                    isSavingRun = true
                ) }
            }
            ActiveRunAction.OnResumeRunClick -> {
                updateState { it.copy(shouldTrack = true) }
            }
            ActiveRunAction.OnToggleRunClick -> {
                updateState { it.copy(
                    hasStartedRunning = true,
                    shouldTrack = !it.shouldTrack
                ) }
            }
            is ActiveRunAction.SubmitLocationPermissionInfo -> {
                hasLocationPermission.value = action.acceptedLocationPermission
                updateState { it.copy(showLocationRationale = action.showLocationRationale) }
            }
            is ActiveRunAction.SubmitNotificationPermissionInfo -> {
                updateState { it.copy(showNotificationRationale = action.showNotificationRationale) }
            }

            ActiveRunAction.DismissRationaleDialog -> {
                updateState { it.copy(
                    showLocationRationale = false,
                    showNotificationRationale = false
                ) }
            }

            is ActiveRunAction.OnRunProcessed -> {
                finishRun(action.mapPictureBytes)
            }
        }
    }

    private fun finishRun(mapPictureBytes: ByteArray) {
        val locations = state.value.runData.locations
        if(locations.isEmpty() || locations.first().size <= 1) {
            updateState { it.copy(
                isSavingRun = false
            ) }
            return
        }

        viewModelScope.launch {
            val run = Run(
                id = null,
                duration = state.value.elapsedTime,
                dateTimeUtc = ZonedDateTime.now()
                    .withZoneSameInstant(ZoneId.of("UTC")),
                distanceMeters = state.value.runData.distanceMeters,
                location = state.value.currentLocation ?: Location(0.0, 0.0),
                maxSpeedKmh = LocationDataCalculator.getMaxSpeedKmh(locations),
                totalElevationMeters = LocationDataCalculator.getTotalElevationMeters(locations),
                mapPictureUrl = null
            )

            runningTracker.finishRun()

            when(val result = runRepository.upsertRun(run, mapPictureBytes)) {
                is com.pratik.core.domain.util.Result.Error -> {
                    eventChannel.send(ActiveRunEvents.Error(result.error.asUiText()))
                }
                is com.pratik.core.domain.util.Result.Success -> {
                    eventChannel.send(ActiveRunEvents.RunSaved)
                }
            }

            updateState { it.copy(
                isSavingRun = false
            ) }
        }
    }

    private fun observeLocationPermission() {
        hasLocationPermission
            .onEach { hasPermission ->
                if(hasPermission) {
                    runningTracker.startObservingLocation()
                } else{
                    runningTracker.stopObservingLocation()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeTrackingState() {
        isTracking
            .onEach { isTracking ->
                runningTracker.setIsTracking(isTracking)
            }
            .launchIn(viewModelScope)
    }

    private fun observeRunningTracker() {
        runningTracker
            .currentLocation
            .onEach { location->
                updateState { it.copy(currentLocation = location?.location) }
            }
            .launchIn(viewModelScope)

        runningTracker
            .runData
            .onEach { runData ->
                updateState { it.copy(runData = runData) }
            }
            .launchIn(viewModelScope)

        runningTracker
            .elapsedTime
            .onEach { elapsedTime ->
                updateState { it.copy(elapsedTime = elapsedTime) }
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        if(!ActiveRunService.isServiceActive) {
            runningTracker.stopObservingLocation()
        }
    }
}