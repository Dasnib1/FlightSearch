package com.example.flightsearch.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flightsearch.R
import com.example.flightsearch.data.Favorite
import com.example.flightsearch.data.IataAndName
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FlightSearchApp(
    modifier: Modifier = Modifier,
    viewModel: FlightSearchViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val favoriteFlights by viewModel.getFavorites().collectAsState(emptyList())
    val airportList by viewModel.autocompleteField().collectAsState(emptyList())
    val uiState by viewModel.uiState.collectAsState()
    val destinationAirports by viewModel.flightsList(uiState.selectedAirport)
        .collectAsState(emptyList())
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    Column {

        Box(
            modifier = modifier.background(Color(android.graphics.Color.parseColor("#067BD6")))
        ) {
            BasicTextField(
                value = stringResource(R.string.app_name),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                onValueChange = { },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = modifier
                .padding(dimensionResource(R.dimen.main_box_padding))
                .clickable(
                    interactionSource = MutableInteractionSource(),
                    indication = null
                ) {
                    focusManager.clearFocus()
                },
        ) {
            Column {
                SearchBar(
                    value = uiState.textInput,
                    placeholder = R.string.search_bar_placeholder,
                    onClearClick = { viewModel.onClearClick() },
                    onValueChange = { viewModel.updateTextInput(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.main_column_spacer)))

                AnimatedVisibility(uiState.textInput.isNotBlank() && !uiState.isAirportSelected) {
                    AutocompleteSuggestions(
                        airportList = airportList,
                        onItemSelected = {
                            coroutineScope.launch {
                                viewModel.flightsList(it).collect { list ->
                                    val flightList: List<IataAndName> = list
                                    viewModel.updateSelectedAirport(it)
                                    viewModel.syncFavoritesWithFlights(
                                        favoriteFlights,
                                        it,
                                        flightList
                                    )
                                }
                            }
                        },
                        modifier = if (airportList.isNotEmpty())
                            Modifier
                                .animateEnterExit(
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                )
                                .padding(bottom = dimensionResource(R.dimen.autocomplete_suggestions_bottom_padding))
                        else
                            Modifier
                    )
                }

                AnimatedVisibility(uiState.textInput.isNotBlank() && uiState.isAirportSelected) {
                    PossibleFlights(
                        selectedAirport = uiState.selectedAirport,
                        destinationAirports = destinationAirports,
                        saveFavorite = {
                            coroutineScope.launch {
                                viewModel.saveFavoriteItem(it)
                            }
                        },
                        deleteFavorite = {
                            coroutineScope.launch {
                                viewModel.deleteFavoriteItem(it)
                            }
                        },
                        isFlightSaved = { viewModel.isSaved(it) },
                        modifier = Modifier.animateEnterExit(
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        )
                    )
                }

                SavedFlights(
                    items = favoriteFlights,
                    deleteItem = { viewModel.deleteFavoriteItem(it) },
                )

            }
        }
    }
}

@Composable
fun SavedFlights(
    items: List<Favorite>,
    deleteItem: (Favorite) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        if (items.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .padding(bottom = dimensionResource(R.dimen.possible_flight_text_bottom_padding))
                    .fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.favorite_routes),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        LazyColumn {
            items(
                items = items.reversed(),
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.card_default_elevation)),
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.background),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen.possible_flight_card_vertical_padding)),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(dimensionResource(R.dimen.possible_flight_card_column_padding))
                                .weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.depart),
                                fontSize = dimensionResource(R.dimen.depart_font_size).value.sp,
                                fontWeight = FontWeight.Light,
                            )

                            Text(
                                text = it.departureCode,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.card_height_spacer)))

                            Text(
                                text = stringResource(R.string.arrive),
                                fontSize = dimensionResource(R.dimen.arrive_font_size).value.sp,
                                fontWeight = FontWeight.Light,
                            )

                            Text(
                                it.destinationCode,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Icon(
                            painter = painterResource(R.drawable.baseline_star_24),
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = null,
                            modifier = Modifier
                                .size(dimensionResource(R.dimen.star_icon_size))
                                .padding(end = dimensionResource(R.dimen.star_icon_end_padding))
                                .clickable {
                                    deleteItem(
                                        Favorite(
                                            departureCode = it.departureCode,
                                            destinationCode = it.destinationCode
                                        )
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    @StringRes placeholder: Int,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    onClearClick: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        singleLine = true,
        onValueChange = onValueChange,
        shape = MaterialTheme.shapes.medium,
        placeholder = if (value.isBlank()) {
            { Text(text = stringResource(placeholder)) }
        } else null,
        leadingIcon =
        {
            Icon(
                painterResource(R.drawable.baseline_search_24),
                contentDescription = null,
            )
        },
        trailingIcon = if (value.isNotBlank()) {
            {
                Icon(
                    painterResource(R.drawable.baseline_clear_24),
                    contentDescription = null,
                    modifier = Modifier.clickable { onClearClick() }
                )
            }
        } else null,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Go
        ),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = modifier
    )
}

@Composable
fun AutocompleteSuggestions(
    airportList: List<IataAndName>,
    onItemSelected: (IataAndName) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(
            items = airportList,
            key = { it.iataCode }
        ) {
            Row(
                modifier = Modifier
                    .padding(vertical = dimensionResource(R.dimen.lazy_column_row_vertical_padding))
                    .clickable {
                        onItemSelected(it)
                    }
            ) {
                Text(
                    text = it.iataCode,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = dimensionResource(R.dimen.iata_code_minimum_width))
                )

                Text(
                    text = it.name,
                    fontWeight = FontWeight.Light,
                )
            }
        }
    }
}

@Composable
fun PossibleFlights(
    destinationAirports: List<IataAndName>,
    selectedAirport: IataAndName,
    saveFavorite: (Favorite) -> Unit,
    isFlightSaved: (Favorite) -> Boolean,
    deleteFavorite: (Favorite) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (destinationAirports.isNotEmpty()) {
            Text(
                text = stringResource(R.string.flights_from, selectedAirport.iataCode),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = dimensionResource(R.dimen.possible_flight_text_bottom_padding))
            )
        }
        LazyColumn {
            items(
                items = destinationAirports,
                key = { it.iataCode }
            ) { destinationAirport ->
                PossibleFlightCard(
                    destinationAirport = destinationAirport,
                    selectedAirport = selectedAirport,
                    saveFavorite = saveFavorite,
                    isFlightSaved = isFlightSaved,
                    deleteFavorite = deleteFavorite,
                    modifier = Modifier.padding(vertical = dimensionResource(R.dimen.possible_flight_card_vertical_padding))
                )
            }
        }
    }
}

@Composable
fun PossibleFlightCard(
    destinationAirport: IataAndName,
    selectedAirport: IataAndName,
    saveFavorite: (Favorite) -> Unit,
    isFlightSaved: (Favorite) -> Boolean,
    deleteFavorite: (Favorite) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.card_default_elevation)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(dimensionResource(R.dimen.possible_flight_card_column_padding))
            ) {
                Text(
                    text = stringResource(R.string.depart),
                    fontSize = dimensionResource(R.dimen.depart_font_size).value.sp,
                    fontWeight = FontWeight.Light,
                )

                Row {
                    Text(
                        text = selectedAirport.iataCode,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = dimensionResource(R.dimen.iata_code_minimum_width)),
                    )

                    Text(
                        text = selectedAirport.name
                    )
                }

                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.card_height_spacer)))

                Text(
                    text = stringResource(R.string.arrive),
                    fontSize = dimensionResource(R.dimen.arrive_font_size).value.sp,
                    fontWeight = FontWeight.Light,
                )

                Row {
                    Text(
                        text = destinationAirport.iataCode,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = dimensionResource(R.dimen.iata_code_minimum_width))
                    )

                    Text(
                        destinationAirport.name
                    )
                }
            }

            Icon(
                painter = painterResource(R.drawable.baseline_star_24),
                contentDescription = null,
                modifier = Modifier
                    .size(dimensionResource(R.dimen.star_icon_size))
                    .padding(end = dimensionResource(R.dimen.star_icon_end_padding))
                    .clickable
                    {
                        if (!isFlightSaved(
                                Favorite(
                                    departureCode = selectedAirport.iataCode,
                                    destinationCode = destinationAirport.iataCode
                                )
                            )
                        )
                            saveFavorite(
                                Favorite(
                                    departureCode = selectedAirport.iataCode,
                                    destinationCode = destinationAirport.iataCode
                                )
                            )
                        else
                            deleteFavorite(
                                Favorite(
                                    departureCode = selectedAirport.iataCode,
                                    destinationCode = destinationAirport.iataCode
                                )
                            )
                    },
                tint = if (isFlightSaved(
                        Favorite(
                            departureCode = selectedAirport.iataCode,
                            destinationCode = destinationAirport.iataCode
                        )
                    )
                )
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

