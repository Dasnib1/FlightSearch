package com.example.flightsearch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flightsearch.data.Favorite
import com.example.flightsearch.data.FlightSearchRepository
import com.example.flightsearch.data.IataAndName
import com.example.flightsearch.data.UserPreferencesRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FlightSearchUIState(
    val textInput: String = "",
    val isAirportSelected: Boolean = false,
    val isDeleteWarning: Boolean = false,
    val selectedAirport: IataAndName = IataAndName(iataCode = "", name = ""),
    val flightSavedStates: MutableMap<Favorite, Boolean> = mutableMapOf(),
)

@OptIn(FlowPreview::class)
class FlightSearchViewModel(
    private val flightSearchRepository: FlightSearchRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FlightSearchUIState()
    )
    val uiState: StateFlow<FlightSearchUIState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    textInput = userPreferencesRepository.textInput.first()
                )
            }
        }
    }

    fun updateTextInput(input: String) {
        _uiState.update {
            it.copy(
                textInput = input, isAirportSelected = false
            )
        }
        viewModelScope.launch {
            userPreferencesRepository.saveTextInput(input)
        }
    }

    fun autocompleteField(): Flow<List<IataAndName>> {
        return if (_uiState.value.textInput.isNotBlank()) flightSearchRepository.getAutocompleteField(
            _uiState.value.textInput.trim()
        ).debounce(500L)
        else emptyFlow()
    }

    fun updateSelectedAirport(updatedSelectedAirport: IataAndName) {
        _uiState.update {
            it.copy(
                selectedAirport = updatedSelectedAirport, isAirportSelected = true
            )
        }
    }

    fun flightsList(selectedAirport: IataAndName): Flow<List<IataAndName>> =
        flightSearchRepository.getFlightsList(selectedAirport.iataCode, selectedAirport.name)

    private fun updateFlightSavedState(favorite: Favorite, newState: Boolean) {
        _uiState.update {
            it.copy(flightSavedStates = _uiState.value.flightSavedStates.toMutableMap().apply {
                this[favorite] = newState
            })
        }
    }

    fun saveFavoriteItem(favorite: Favorite) {
        updateFlightSavedState(favorite, true)
        viewModelScope.launch {
            flightSearchRepository.insertFavorite(favorite)
        }
    }

    fun isSaved(favorite: Favorite): Boolean {
        return _uiState.value.flightSavedStates[favorite] == true
    }

    fun getFavorites(): Flow<List<Favorite>> = flightSearchRepository.getFavorites()

    fun deleteFavoriteItem(favorite: Favorite) {
        if (_uiState.value.flightSavedStates[favorite] == true) updateFlightSavedState(
            favorite,
            false
        )
        viewModelScope.launch {
            flightSearchRepository.deleteFavorite(favorite.departureCode, favorite.destinationCode)
        }
    }

    fun onClearClick() {
        _uiState.update {
            it.copy(
                textInput = ""
            )
        }
        viewModelScope.launch {
            userPreferencesRepository.saveTextInput(_uiState.value.textInput)
        }
    }

    fun syncFavoritesWithFlights(
        favorites: List<Favorite>,
        selectedAirport: IataAndName,
        destinationAirports: List<IataAndName>
    ) {
        for (favorite in favorites) for (destinationAirport in destinationAirports) {
            if (favorite.departureCode == selectedAirport.iataCode && favorite.destinationCode == destinationAirport.iataCode) updateFlightSavedState(
                Favorite(
                    departureCode = selectedAirport.iataCode,
                    destinationCode = destinationAirport.iataCode
                ), true
            )
        }
    }
}