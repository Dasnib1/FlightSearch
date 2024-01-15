package com.example.flightsearch.data

import kotlinx.coroutines.flow.Flow

class OfflineFlightSearchRepo(private val airportDao: AirportDao): FlightSearchRepository {
    override fun getAutocompleteField(input: String): Flow<List<IataAndName>> =
        airportDao.getAutocompleteField(input)

    override fun getFlightsList(name: String, iataCode: String): Flow<List<IataAndName>> =
        airportDao.getFlightsList(name, iataCode)

    override suspend fun insertFavorite(favorite: Favorite) =
        airportDao.insertFavorite(favorite)

    override suspend fun deleteFavorite(departureCode: String, destinationCode: String) =
        airportDao.deleteFavorite(departureCode, destinationCode)

    override fun getFavorites(): Flow<List<Favorite>> =
        airportDao.getFavorites()
}