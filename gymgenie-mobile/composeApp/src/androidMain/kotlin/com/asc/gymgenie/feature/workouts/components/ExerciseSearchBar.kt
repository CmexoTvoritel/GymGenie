package com.asc.gymgenie.feature.workouts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.asc.gymgenie.ui.theme.OnSurfaceVariant

@Composable
fun ExerciseSearchBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keep the latest query and search callback so the focus-change listener
    // never captures stale values from the first composition.
    val currentQuery by rememberUpdatedState(searchQuery)
    val currentOnSearch by rememberUpdatedState(onSearch)

    // Guards against the double-search that would otherwise happen when the
    // user taps IME Search: the keyboard action fires once, then the IME
    // collapses and the field loses focus, which would re-fire the search via
    // [onFocusChanged]. We mark the IME-triggered case and swallow the next
    // focus-loss event instead of running a second network call.
    var searchTriggeredByIme by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(Color.White),
    ) {
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Поиск упражнений...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Поиск",
                    tint = OnSurfaceVariant,
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Очистить",
                            tint = OnSurfaceVariant,
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                searchTriggeredByIme = true
                onSearch()
            }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    // Trigger search when the field loses focus with a non-empty
                    // query — covers the IME being dismissed via Back as well
                    // as the user tapping elsewhere on the screen. The IME
                    // Search action already issued a request, so we skip the
                    // follow-up focus-loss event in that case.
                    if (focusState.isFocused) {
                        searchTriggeredByIme = false
                    } else if (currentQuery.isNotEmpty()) {
                        if (searchTriggeredByIme) {
                            searchTriggeredByIme = false
                        } else {
                            currentOnSearch()
                        }
                    }
                },
        )
    }
}
