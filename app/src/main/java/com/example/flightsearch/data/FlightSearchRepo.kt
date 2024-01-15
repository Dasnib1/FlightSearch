package com.example.flightsearch.data

import kotlinx.coroutines.flow.Flow

interface FlightSearchRepository {
    fun getAutocompleteField(input: String): Flow<List<IataAndName>>

    fun getFlightsList(name: String, iataCode: String): Flow<List<IataAndName>>

    suspend fun insertFavorite(favorite: Favorite)

    fun getFavorites(): Flow<List<Favorite>>

    suspend fun deleteFavorite(departureCode: String, destinationCode: String)
}