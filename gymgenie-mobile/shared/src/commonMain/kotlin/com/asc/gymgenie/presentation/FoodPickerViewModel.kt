package com.asc.gymgenie.presentation

import com.asc.gymgenie.nutrition.FoodCategory
import com.asc.gymgenie.nutrition.FoodProduct
import com.asc.gymgenie.nutrition.FoodProductApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FoodPickerUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val allProducts: List<FoodProduct> = emptyList(),
    val filteredProducts: List<FoodProduct> = emptyList(),
    val selectedCategory: FoodCategory? = null,
    val searchQuery: String = "",
    val selectedProduct: FoodProduct? = null,
    val amountGrams: String = DEFAULT_AMOUNT_GRAMS,
) {

    val parsedAmountGrams: Double?
        get() = amountGrams.replace(',', '.').trim().toDoubleOrNull()?.takeIf { it > 0.0 }

    val canConfirmAmount: Boolean
        get() = selectedProduct != null && (parsedAmountGrams ?: 0.0) > 0.0

    companion object {
        const val DEFAULT_AMOUNT_GRAMS = "100"
    }
}

class FoodPickerViewModel(
    private val foodProductApi: FoodProductApi,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(FoodPickerUiState())
    val state: StateFlow<FoodPickerUiState> = _state.asStateFlow()

    fun load() {
        val current = _state.value
        if (current.isLoading) return
        if (current.allProducts.isNotEmpty() && current.errorMessage == null) return

        _state.update { it.copy(isLoading = true, errorMessage = null) }
        scope.launch {
            foodProductApi.searchProducts().fold(
                onSuccess = { products ->
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            allProducts = products,
                            filteredProducts = applyFilters(
                                products = products,
                                query = state.searchQuery,
                                category = state.selectedCategory,
                            ),
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Не удалось загрузить продукты: ${error.message}",
                        )
                    }
                },
            )
        }
    }

    fun retry() {
        _state.update { it.copy(allProducts = emptyList(), errorMessage = null) }
        load()
    }

    fun onSearchQueryChange(query: String) {
        _state.update { state ->
            state.copy(
                searchQuery = query,
                filteredProducts = applyFilters(
                    products = state.allProducts,
                    query = query,
                    category = state.selectedCategory,
                ),
            )
        }
    }

    fun onCategorySelected(category: FoodCategory?) {
        _state.update { state ->
            state.copy(
                selectedCategory = category,
                filteredProducts = applyFilters(
                    products = state.allProducts,
                    query = state.searchQuery,
                    category = category,
                ),
            )
        }
    }

    fun selectProduct(product: FoodProduct) {
        _state.update {
            it.copy(
                selectedProduct = product,
                amountGrams = FoodPickerUiState.DEFAULT_AMOUNT_GRAMS,
            )
        }
    }

    fun dismissProductDetail() {
        _state.update { it.copy(selectedProduct = null) }
    }

    fun onAmountChange(raw: String) {

        val sanitized = raw
            .filter { it.isDigit() || it == '.' || it == ',' }
            .replace(',', '.')
            .let { value ->
                val firstDot = value.indexOf('.')
                if (firstDot < 0) value
                else value.substring(0, firstDot + 1) +
                    value.substring(firstDot + 1).replace(".", "")
            }
            .take(MAX_AMOUNT_INPUT_LENGTH)
        _state.update { it.copy(amountGrams = sanitized) }
    }

    fun onAmountPresetSelected(grams: Int) {
        _state.update { it.copy(amountGrams = grams.toString()) }
    }

    fun onCleared() {
        scope.cancel()
    }

    private fun applyFilters(
        products: List<FoodProduct>,
        query: String,
        category: FoodCategory?,
    ): List<FoodProduct> {
        val trimmedQuery = query.trim()
        return products.asSequence()
            .filter { product -> category == null || product.category == category }
            .filter { product ->
                if (trimmedQuery.isEmpty()) return@filter true
                product.nameRu.contains(trimmedQuery, ignoreCase = true) ||
                    (product.nameEn?.contains(trimmedQuery, ignoreCase = true) == true)
            }
            .toList()
    }

    companion object {
        private const val MAX_AMOUNT_INPUT_LENGTH = 6
    }
}
