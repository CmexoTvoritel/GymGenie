package com.asc.gymgenie.feature.food_picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asc.gymgenie.nutrition.FoodCategory
import com.asc.gymgenie.nutrition.FoodProduct
import com.asc.gymgenie.nutrition.FoodProductApi
import com.asc.gymgenie.nutrition.macrosForGrams
import com.asc.gymgenie.presentation.FoodPickerViewModel
import com.asc.gymgenie.ui.theme.AccentOrange
import com.asc.gymgenie.ui.theme.DeepInk
import com.asc.gymgenie.ui.theme.MutedText
import com.asc.gymgenie.ui.theme.SoftCard
import com.asc.gymgenie.ui.theme.WarmOffWhite
import org.koin.core.context.GlobalContext

private val CardBorder = Color(0xFFEDEDEF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodPickerScreen(
    onProductSelected: (product: FoodProduct, amountGrams: Double) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val koin = remember { GlobalContext.get() }
    val viewModel = remember { FoodPickerViewModel(foodProductApi = koin.get<FoodProductApi>()) }
    DisposableEffect(Unit) { onDispose { viewModel.onCleared() } }

    LaunchedEffect(Unit) { viewModel.load() }

    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmOffWhite),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            PickerHeader(onClose = onClose)

            SearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
            )

            CategoryChips(
                selected = state.selectedCategory,
                onSelect = viewModel::onCategorySelected,
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                state.isLoading -> LoadingState()
                state.errorMessage != null -> ErrorState(
                    message = state.errorMessage!!,
                    onRetry = viewModel::retry,
                )
                state.filteredProducts.isEmpty() -> EmptyState()
                else -> ProductList(
                    products = state.filteredProducts,
                    onProductTap = viewModel::selectProduct,
                )
            }
        }

        if (state.selectedProduct != null) {
            ModalBottomSheet(
                onDismissRequest = viewModel::dismissProductDetail,
                sheetState = sheetState,
                containerColor = WarmOffWhite,
            ) {
                ProductDetailSheet(
                    product = state.selectedProduct!!,
                    amountGrams = state.amountGrams,
                    canConfirm = state.canConfirmAmount,
                    onAmountChange = viewModel::onAmountChange,
                    onPresetSelected = viewModel::onAmountPresetSelected,
                    onConfirm = {
                        val grams = state.parsedAmountGrams ?: return@ProductDetailSheet
                        onProductSelected(state.selectedProduct!!, grams)
                        viewModel.dismissProductDetail()
                        onClose()
                    },
                )
            }
        }
    }
}

@Composable
private fun PickerHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.5.dp, CardBorder, CircleShape)
                .clickable { onClose() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Назад",
                tint = DeepInk,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Добавить продукт",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepInk,
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Поиск продукта...", color = MutedText) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentOrange,
            unfocusedBorderColor = CardBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            cursorColor = AccentOrange,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun CategoryChips(
    selected: FoodCategory?,
    onSelect: (FoodCategory?) -> Unit,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoryChip(label = "Все", isSelected = selected == null, onClick = { onSelect(null) })
        FoodCategory.entries.forEach { category ->
            CategoryChip(
                label = category.displayName(),
                isSelected = selected == category,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) AccentOrange else Color.White
    val textColor = if (isSelected) Color.White else DeepInk
    val borderColor = if (isSelected) AccentOrange else CardBorder

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
        )
    }
}

@Composable
private fun ProductList(
    products: List<FoodProduct>,
    onProductTap: (FoodProduct) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items = products, key = { it.id }) { product ->
            ProductRow(product = product, onTap = { onProductTap(product) })
        }
        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun ProductRow(product: FoodProduct, onTap: () -> Unit) {
    val bg = categoryBg(product.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.5.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable { onTap() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = product.emoji ?: product.category.defaultEmoji(), fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.nameRu,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DeepInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = product.category.displayName(),
                fontSize = 12.sp,
                color = MutedText,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = product.caloriesPer100g.toInt().toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepInk,
            )
            Text(
                text = "ккал/100г",
                fontSize = 10.sp,
                color = MutedText,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ProductDetailSheet(
    product: FoodProduct,
    amountGrams: String,
    canConfirm: Boolean,
    onAmountChange: (String) -> Unit,
    onPresetSelected: (Int) -> Unit,
    onConfirm: () -> Unit,
) {
    val parsedGrams = amountGrams.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: 0.0
    val macros = if (parsedGrams > 0) product.macrosForGrams(parsedGrams) else null
    val bg = categoryBg(product.category)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = product.emoji ?: product.category.defaultEmoji(), fontSize = 26.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = product.nameRu,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepInk,
                    maxLines = 2,
                )
                Text(text = "на 100 г", fontSize = 12.sp, color = MutedText)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroMiniCard(
                label = "Калории",
                value = "${product.caloriesPer100g.toInt()} ккал",
                bg = Color(0xFFFFF4E6),
                textColor = Color(0xFFE07B00),
                modifier = Modifier.weight(1f),
            )
            MacroMiniCard(
                label = "Белки",
                value = "${fmtMacro(product.proteinPer100g)}г",
                bg = Color(0xFFE1F1FF),
                textColor = Color(0xFF0A84FF),
                modifier = Modifier.weight(1f),
            )
            MacroMiniCard(
                label = "Жиры",
                value = "${fmtMacro(product.fatPer100g)}г",
                bg = Color(0xFFFFEAEA),
                textColor = Color(0xFFFF6B6B),
                modifier = Modifier.weight(1f),
            )
            MacroMiniCard(
                label = "Углеводы",
                value = "${fmtMacro(product.carbsPer100g)}г",
                bg = Color(0xFFE8F7E8),
                textColor = Color(0xFF34C759),
                modifier = Modifier.weight(1f),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "СКОЛЬКО ГРАММ?",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedText,
                letterSpacing = 0.5.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(50, 100, 150, 200, 300).forEach { preset ->
                    val isSelected = amountGrams.trim() == preset.toString()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) AccentOrange else SoftCard)
                            .clickable { onPresetSelected(preset) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${preset}г",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else DeepInk,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = amountGrams,
                onValueChange = onAmountChange,
                placeholder = { Text("Введите вес, г", color = MutedText) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                suffix = { Text("г", color = MutedText) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentOrange,
                    unfocusedBorderColor = CardBorder,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = AccentOrange,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (macros != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SoftCard)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "≈ ${macros.calories.toInt()} ккал  ·  Б ${fmtMacro(macros.proteinG)}г  ·  Ж ${fmtMacro(macros.fatG)}г  ·  У ${fmtMacro(macros.carbsG)}г",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepInk,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (canConfirm) AccentOrange else SoftCard)
                .clickable(enabled = canConfirm) { onConfirm() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Добавить в блюдо",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (canConfirm) Color.White else MutedText,
            )
        }
    }
}

@Composable
private fun MacroMiniCard(
    label: String,
    value: String,
    bg: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AccentOrange)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "⚠️", fontSize = 36.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = message, fontSize = 14.sp, color = MutedText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Повторить", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🔍", fontSize = 36.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Продукт не найден", fontSize = 15.sp, color = MutedText)
        }
    }
}

private fun fmtMacro(value: Double): String {
    return if (value == value.toLong().toDouble()) value.toLong().toString()
    else "%.1f".format(value)
}

private fun FoodCategory.defaultEmoji(): String = when (this) {
    FoodCategory.MEAT -> "🍗"
    FoodCategory.FISH -> "🐟"
    FoodCategory.DAIRY -> "🥛"
    FoodCategory.EGGS -> "🥚"
    FoodCategory.GRAINS -> "🌾"
    FoodCategory.LEGUMES -> "🫘"
    FoodCategory.VEGETABLES -> "🥦"
    FoodCategory.FRUITS -> "🍎"
    FoodCategory.NUTS_SEEDS -> "🥜"
    FoodCategory.OILS -> "🫙"
    FoodCategory.OTHER -> "🍴"
}

private fun categoryBg(category: FoodCategory): Color = when (category) {
    FoodCategory.MEAT -> Color(0xFFFFE8E0)
    FoodCategory.FISH -> Color(0xFFE0F0FF)
    FoodCategory.DAIRY -> Color(0xFFFFF8E8)
    FoodCategory.EGGS -> Color(0xFFFFF4D6)
    FoodCategory.GRAINS -> Color(0xFFFFF0D6)
    FoodCategory.LEGUMES -> Color(0xFFE8F7E8)
    FoodCategory.VEGETABLES -> Color(0xFFE0F5E8)
    FoodCategory.FRUITS -> Color(0xFFFDE8F0)
    FoodCategory.NUTS_SEEDS -> Color(0xFFF5EDE0)
    FoodCategory.OILS -> Color(0xFFFFF9E0)
    FoodCategory.OTHER -> Color(0xFFF3F2EF)
}
