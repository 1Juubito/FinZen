package com.finzen.app.ui.transaction

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finzen.app.data.model.RecurrenceFrequency
import com.finzen.app.data.model.TransactionType
import com.finzen.app.di.AppViewModelProvider
import com.finzen.app.ui.components.CategoryAvatar
import com.finzen.app.ui.components.CompactDatePickerDialog
import com.finzen.app.ui.components.ListDivider
import com.finzen.app.ui.components.NeoField
import com.finzen.app.ui.theme.FinanceTheme
import com.finzen.app.ui.theme.NeoCard
import com.finzen.app.ui.theme.colorFromHex
import com.finzen.app.util.DateUtils
import java.time.LocalDate

private enum class PickerSheet { NONE, CATEGORY, ACCOUNT, TO_ACCOUNT, PAYMENT, INSTALLMENTS, REPEAT, REPEAT_COUNT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditTransactionViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf(PickerSheet.NONE) }
    var showDatePicker by remember { mutableStateOf(false) }

    var descriptionFocused by remember { mutableStateOf(false) }
    var notesFocused by remember { mutableStateOf(false) }
    val typingText = descriptionFocused || notesFocused

    val focusManager = LocalFocusManager.current
    var keypadVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    val accentColor = when (state.type) {
        TransactionType.INCOME -> FinanceTheme.colors.income
        TransactionType.EXPENSE -> FinanceTheme.colors.expense
        TransactionType.TRANSFER -> FinanceTheme.colors.transfer
    }

    val todayMillis = remember { DateUtils.localDateToMillis(LocalDate.now()) }
    val yesterdayMillis = remember { DateUtils.localDateToMillis(LocalDate.now().minusDays(1)) }
    val selectedDay = DateUtils.localDateToMillis(DateUtils.toLocalDate(state.date))
    val isToday = selectedDay == todayMillis
    val isYesterday = selectedDay == yesterdayMillis
    val isCustomDate = !isToday && !isYesterday

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding(),
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
            }
            Text(
                text = (if (state.isEditing) "Editar " else "Nova ") + state.type.label.lowercase(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (state.isEditing) {
                IconButton(onClick = viewModel::delete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Excluir", tint = FinanceTheme.colors.expense)
                }
            }
        }

        if (!state.isEditing) {
            TypeSegmented(
                selected = state.type,
                accent = accentColor,
                onSelect = viewModel::setType,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        NeoCard(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .fillMaxWidth()
                .clickable {
                    focusManager.clearFocus()
                    keypadVisible = true
                },
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)) {
                Text(
                    text = "Valor da ${state.type.label.lowercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = state.amountFormatted,
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor,
                        maxLines = 1,
                    )
                    if (keypadVisible) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .width(2.dp)
                                .height(28.dp)
                                .background(accentColor),
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 6.dp),
        ) {

            NeoCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth(),
            ) {
            if (state.type != TransactionType.TRANSFER) {
                DetailRow(icon = Icons.Rounded.Check) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (state.type == TransactionType.INCOME) "Recebido" else "Pago",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (state.isPaid) "Transação efetivada" else "Ainda pendente",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.isPaid, onCheckedChange = { viewModel.togglePaid() })
                }
                ListDivider(60.dp)
            }

            DetailRow(icon = Icons.Rounded.CalendarMonth) {
                DateChip("Hoje", isToday, accentColor) { viewModel.setDate(DateUtils.now()) }
                Spacer(Modifier.width(8.dp))
                DateChip("Ontem", isYesterday, accentColor) { viewModel.setDate(yesterdayMillis) }
                Spacer(Modifier.width(8.dp))
                DateChip(
                    label = if (isCustomDate) DateUtils.dayMonthLabel(state.date) else "Outros…",
                    selected = isCustomDate,
                    accent = accentColor,
                ) { showDatePicker = true }
            }
            ListDivider(60.dp)

            if (state.type != TransactionType.TRANSFER) {
                DetailRow(icon = Icons.Rounded.Category, onClick = { sheet = PickerSheet.CATEGORY }) {
                    val category = state.selectedCategory
                    if (category != null) {
                        SelectorPill(category.colorHex, category.iconKey, category.name)
                    } else {
                        PlaceholderText("Selecionar categoria")
                    }
                    Spacer(Modifier.weight(1f))
                    ChevronEnd()
                }
                ListDivider(60.dp)
            }

            when (state.type) {
                TransactionType.TRANSFER -> {
                    DetailRow(icon = Icons.Rounded.Wallet, onClick = { sheet = PickerSheet.ACCOUNT }) {
                        val a = state.selectedAccount
                        if (a != null) SelectorPill(a.colorHex, a.iconKey, a.name) else PlaceholderText("Conta de origem")
                        Spacer(Modifier.weight(1f))
                        ChevronEnd()
                    }
                    ListDivider(60.dp)
                    DetailRow(icon = Icons.Rounded.Wallet, onClick = { sheet = PickerSheet.TO_ACCOUNT }) {
                        val a = state.selectedToAccount
                        if (a != null) SelectorPill(a.colorHex, a.iconKey, a.name) else PlaceholderText("Conta de destino")
                        Spacer(Modifier.weight(1f))
                        ChevronEnd()
                    }
                    ListDivider(60.dp)
                }
                TransactionType.INCOME -> {
                    DetailRow(icon = Icons.Rounded.Wallet, onClick = { sheet = PickerSheet.ACCOUNT }) {
                        val a = state.selectedAccount
                        if (a != null) SelectorPill(a.colorHex, a.iconKey, a.name) else PlaceholderText("Selecionar conta")
                        Spacer(Modifier.weight(1f))
                        ChevronEnd()
                    }
                    ListDivider(60.dp)
                }
                TransactionType.EXPENSE -> {
                    DetailRow(
                        icon = if (state.creditCardId != null) Icons.Rounded.CreditCard else Icons.Rounded.Wallet,
                        onClick = { sheet = PickerSheet.PAYMENT },
                    ) {
                        val card = state.selectedCard
                        val account = state.selectedAccount
                        when {
                            card != null -> SelectorPill(card.colorHex, "card", card.name)
                            account != null -> SelectorPill(account.colorHex, account.iconKey, account.name)
                            else -> PlaceholderText("Forma de pagamento")
                        }
                        Spacer(Modifier.weight(1f))
                        ChevronEnd()
                    }
                    ListDivider(60.dp)
                }
            }

            if (state.showInstallments) {
                DetailRow(icon = Icons.Rounded.Layers, onClick = { sheet = PickerSheet.INSTALLMENTS }) {
                    Text(
                        text = if (state.installments <= 1) "À vista" else "${state.installments}x",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.weight(1f))
                    ChevronEnd()
                }
                ListDivider(60.dp)
            }

            if (state.canRepeat && !state.isEditing) {
                DetailRow(icon = Icons.Rounded.Repeat, onClick = { sheet = PickerSheet.REPEAT }) {
                    Text(
                        text = state.repeatLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (state.repeat == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Spacer(Modifier.weight(1f))
                    ChevronEnd()
                }
                ListDivider(60.dp)
            }

            if (state.showRepeatCount && !state.isEditing) {
                DetailRow(icon = Icons.Rounded.Numbers, onClick = { sheet = PickerSheet.REPEAT_COUNT }) {
                    Text(
                        text = state.repeatCountLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.weight(1f))
                    ChevronEnd()
                }
                ListDivider(60.dp)
            }
            }

            NeoField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                label = "Descrição",
                singleLine = true,
                onFocusChanged = { focused ->
                    descriptionFocused = focused
                    if (focused) keypadVisible = false
                },
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
            )

            NeoField(
                value = state.notes,
                onValueChange = viewModel::setNotes,
                label = "Observações",
                onFocusChanged = { focused ->
                    notesFocused = focused
                    if (focused) keypadVisible = false
                },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )

            Spacer(Modifier.height(8.dp))
        }

        if (keypadVisible && !typingText) {
            ListDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { keypadVisible = false }) {
                    Text(
                        text = "Concluir",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor,
                    )
                }
            }
            NumericKeypad(
                onDigit = viewModel::appendDigit,
                onBackspace = viewModel::backspace,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        Button(
            onClick = viewModel::save,
            enabled = state.canSave,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(54.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = if (state.isEditing) "Salvar alterações" else "Adicionar transação",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
        }
    }

    if (sheet != PickerSheet.NONE) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { sheet = PickerSheet.NONE }, sheetState = sheetState) {
            when (sheet) {
                PickerSheet.CATEGORY -> SelectionSheet(title = "Categoria") {
                    state.categories.forEach { category ->
                        SelectionRow(
                            colorHex = category.colorHex,
                            iconKey = category.iconKey,
                            label = category.name,
                            selected = category.id == state.categoryId,
                        ) {
                            viewModel.selectCategory(category.id)
                            sheet = PickerSheet.NONE
                        }
                    }
                }
                PickerSheet.ACCOUNT -> SelectionSheet(title = "Conta") {
                    state.accounts.forEach { account ->
                        SelectionRow(
                            colorHex = account.colorHex,
                            iconKey = account.iconKey,
                            label = account.name,
                            selected = account.id == state.accountId,
                        ) {
                            viewModel.selectAccount(account.id)
                            sheet = PickerSheet.NONE
                        }
                    }
                }
                PickerSheet.TO_ACCOUNT -> SelectionSheet(title = "Transferir para") {
                    state.accounts.forEach { account ->
                        SelectionRow(
                            colorHex = account.colorHex,
                            iconKey = account.iconKey,
                            label = account.name,
                            selected = account.id == state.toAccountId,
                        ) {
                            viewModel.selectToAccount(account.id)
                            sheet = PickerSheet.NONE
                        }
                    }
                }
                PickerSheet.PAYMENT -> SelectionSheet(title = "Forma de pagamento") {
                    Text(
                        "Contas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                    state.accounts.forEach { account ->
                        SelectionRow(
                            colorHex = account.colorHex,
                            iconKey = account.iconKey,
                            label = account.name,
                            selected = account.id == state.accountId,
                        ) {
                            viewModel.selectAccount(account.id)
                            sheet = PickerSheet.NONE
                        }
                    }
                    if (state.cards.isNotEmpty()) {
                        Text(
                            "Cartões de crédito",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        )
                        state.cards.forEach { card ->
                            SelectionRow(
                                colorHex = card.colorHex,
                                iconKey = "card",
                                label = card.name,
                                selected = card.id == state.creditCardId,
                            ) {
                                viewModel.selectCard(card.id)
                                sheet = PickerSheet.NONE
                            }
                        }
                    }
                }
                PickerSheet.INSTALLMENTS -> SelectionSheet(title = "Parcelas") {
                    listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 18, 24).forEach { count ->
                        SelectionRow(
                            colorHex = null,
                            iconKey = "card",
                            label = if (count == 1) "À vista" else "${count}x",
                            selected = count == state.installments,
                        ) {
                            viewModel.setInstallments(count)
                            sheet = PickerSheet.NONE
                        }
                    }
                }
                PickerSheet.REPEAT -> SelectionSheet(title = "Repetir") {
                    SelectionRow(
                        colorHex = null,
                        iconKey = "calendar",
                        label = "Não repetir",
                        selected = state.repeat == null,
                    ) {
                        viewModel.setRepeat(null)
                        sheet = PickerSheet.NONE
                    }
                    RecurrenceFrequency.values().forEach { frequency ->
                        SelectionRow(
                            colorHex = null,
                            iconKey = "calendar",
                            label = frequency.label,
                            selected = state.repeat == frequency,
                        ) {
                            viewModel.setRepeat(frequency)
                            sheet = PickerSheet.NONE
                        }
                    }
                }
                PickerSheet.REPEAT_COUNT -> SelectionSheet(title = "Repetir quantas vezes?") {
                    SelectionRow(
                        colorHex = null,
                        iconKey = "calendar",
                        label = "Para sempre",
                        selected = state.repeatCount == null,
                    ) {
                        viewModel.setRepeatCount(null)
                        sheet = PickerSheet.NONE
                    }
                    listOf(2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 18, 24, 36).forEach { count ->
                        SelectionRow(
                            colorHex = null,
                            iconKey = "calendar",
                            label = "$count vezes",
                            selected = state.repeatCount == count,
                        ) {
                            viewModel.setRepeatCount(count)
                            sheet = PickerSheet.NONE
                        }
                    }
                }
                PickerSheet.NONE -> Unit
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        CompactDatePickerDialog(
            initialMillis = state.date,
            onConfirm = { viewModel.setDate(it) },
            onDismiss = { showDatePicker = false },
            accent = accentColor,
        )
    }
}

@Composable
private fun TypeSegmented(
    selected: TransactionType,
    accent: Color,
    onSelect: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TransactionType.values().forEach { type ->
            val isSel = type == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSel) accent.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(type) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = type.label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSel) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val base = Modifier.fillMaxWidth()
    Row(
        modifier = (if (onClick != null) base.clickable { onClick() } else base)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(18.dp))
        content()
    }
}

@Composable
private fun ChevronEnd() {
    Icon(
        Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp),
    )
}

@Composable
private fun PlaceholderText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun SelectorPill(colorHex: String?, iconKey: String?, label: String) {
    val color = colorHex?.let { colorFromHex(it) } ?: MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryAvatar(colorHex = colorHex, iconKey = iconKey, size = 26.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DateChip(label: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) accent else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("000", "0", "<"),
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->

                    val keyColor = if (key == "<") {
                        MaterialTheme.colorScheme.surfaceContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(keyColor)
                            .clickable {
                                when (key) {
                                    "<" -> onBackspace()
                                    "000" -> repeat(3) { onDigit('0') }
                                    else -> onDigit(key.first())
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key == "<") {
                            Icon(Icons.Rounded.Backspace, contentDescription = "Apagar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionSheet(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            content()
        }
    }
}

@Composable
private fun SelectionRow(
    colorHex: String?,
    iconKey: String?,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(colorHex = colorHex, iconKey = iconKey, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
