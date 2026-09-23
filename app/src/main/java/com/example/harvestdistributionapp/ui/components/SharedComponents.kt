package com.example.harvestdistributionapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.harvestdistributionapp.data.AvailabilityStatus
import com.example.harvestdistributionapp.data.InputValidator
import com.example.harvestdistributionapp.data.RequestStatus
import com.example.harvestdistributionapp.data.epochMillisToIsoDate

@Composable
fun M3Logo(size: Dp = 32.dp, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size * 0.3f),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Agriculture,
                contentDescription = null,
                modifier = Modifier.size(size * 0.6f),
                tint = color
            )
        }
    }
}

@Composable
fun AvailChip(status: AvailabilityStatus) {
    val background: Color
    val foreground: Color
    val label: String
    val icon: ImageVector
    when (status) {
        AvailabilityStatus.AVAILABLE -> {
            background = Color(0xFFD3EDCA)
            foreground = Color(0xFF1B4F10)
            label = "Disponible"
            icon = Icons.Default.CheckCircle
        }
        AvailabilityStatus.LIMITED -> {
            background = MaterialTheme.colorScheme.tertiaryContainer
            foreground = MaterialTheme.colorScheme.onTertiaryContainer
            label = "Limitado"
            icon = Icons.Default.Error
        }
        AvailabilityStatus.OUT -> {
            background = MaterialTheme.colorScheme.errorContainer
            foreground = MaterialTheme.colorScheme.onErrorContainer
            label = "Agotado"
            icon = Icons.Default.Cancel
        }
    }
    Surface(color = background, shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = foreground)
            Text(label, style = MaterialTheme.typography.labelMedium, color = foreground)
        }
    }
}

@Composable
fun ReqChip(status: RequestStatus) {
    val background: Color
    val foreground: Color
    val label: String
    val icon: ImageVector
    when (status) {
        RequestStatus.PENDING -> {
            background = MaterialTheme.colorScheme.tertiaryContainer
            foreground = MaterialTheme.colorScheme.onTertiaryContainer
            label = "Pendiente"
            icon = Icons.Default.Schedule
        }
        RequestStatus.ACCEPTED -> {
            background = Color(0xFFD3EDCA)
            foreground = Color(0xFF1B4F10)
            label = "Aceptada"
            icon = Icons.Default.CheckCircle
        }
        RequestStatus.REJECTED -> {
            background = MaterialTheme.colorScheme.errorContainer
            foreground = MaterialTheme.colorScheme.onErrorContainer
            label = "Rechazada"
            icon = Icons.Default.Cancel
        }
    }
    Surface(color = background, shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = foreground)
            Text(label, style = MaterialTheme.typography.labelMedium, color = foreground)
        }
    }
}

@Composable
fun FilledBtn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    testTag: String? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        enabled = enabled,
        shape = CircleShape
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TonalBtn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    testTag: String? = null
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        enabled = enabled,
        shape = CircleShape
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun OutlinedBtn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    testTag: String? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun TextBtn(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    testTag: String? = null
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ExtFAB(label: String, icon: ImageVector, onClick: () -> Unit, testTag: String? = null) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(16.dp),
        icon = { Icon(icon, contentDescription = null) },
        text = { Text(label, style = MaterialTheme.typography.labelLarge) }
    )
}

@Composable
fun M3Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    type: String = "text",
    trailingIcon: ImageVector? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
    testTag: String? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        visualTransformation = if (type == "password" && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = when (type) {
                "email" -> KeyboardType.Email
                "password" -> KeyboardType.Password
                "number" -> KeyboardType.Number
                "decimal" -> KeyboardType.Decimal
                else -> KeyboardType.Text
            }
        ),
        trailingIcon = {
            if (type == "password") {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            } else if (trailingIcon != null) {
                Icon(trailingIcon, contentDescription = null)
            }
        },
        readOnly = readOnly,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText?.let { message -> { Text(message) } },
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    var showPicker by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        M3Field(
            label = label,
            value = value,
            onValueChange = {},
            trailingIcon = Icons.Default.CalendarMonth,
            readOnly = true
        )
        Box(
            Modifier
                .matchParentSize()
                .testTag(testTag)
                .clickable { showPicker = true }
        )
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onValueChange(epochMillisToIsoDate(millis))
                    }
                    showPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = pickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipM3(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    testTag: String? = null
) {
    FilterChip(
        selected = active,
        onClick = onClick,
        modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier,
        label = { Text(label) },
        leadingIcon = when {
            active -> ({ Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) })
            icon != null -> ({ Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) })
            else -> null
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallTopBarM3(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(message, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
    }
}

@Composable
fun InfoBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(message, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center)
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.Inventory2,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    testTag: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onActionClick != null) {
            Spacer(Modifier.height(4.dp))
            TonalBtn(
                text = actionLabel,
                onClick = onActionClick,
                icon = actionIcon,
                modifier = Modifier.widthIn(max = 240.dp),
                testTag = testTag ?: "empty_state_action"
            )
        }
    }
}

@Composable
fun QuantitySelector(
    quantity: Int,
    unit: String,
    onQuantityChange: (Int) -> Unit,
    minimum: Int = 1,
    maximum: Int? = null,
    step: Int = 5,
    testTagPrefix: String = "quantity"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(
            onClick = { onQuantityChange(InputValidator.decrementQuantity(quantity, step, minimum)) },
            enabled = quantity > minimum,
            modifier = Modifier
                .size(48.dp)
                .testTag("${testTagPrefix}_minus")
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
        ) { Icon(Icons.Default.Remove, contentDescription = "Disminuir cantidad") }
        Surface(
            modifier = Modifier.weight(1f).testTag("${testTagPrefix}_value"),
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                "$quantity $unit",
                modifier = Modifier.padding(14.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge
            )
        }
        val canIncrease = maximum == null || quantity < maximum
        IconButton(
            onClick = { onQuantityChange((quantity + step).let { next -> if (maximum == null) next else next.coerceAtMost(maximum) }) },
            enabled = canIncrease,
            modifier = Modifier
                .size(48.dp)
                .testTag("${testTagPrefix}_plus")
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        ) { Icon(Icons.Default.Add, contentDescription = "Aumentar cantidad", tint = MaterialTheme.colorScheme.onPrimary) }
    }
}

/**
 * Componente Composable para solicitar el permiso POST_NOTIFICATIONS
 * en tiempo de ejecución para dispositivos con Android 13 (API 33) o superior.
 */
@Composable
fun NotificationPermissionHandler() {
    val context = LocalContext.current

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            // Puedes manejar aquí la respuesta del usuario si es necesario
        }

        LaunchedEffect(Unit) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                launcher.launch(permission)
            }
        }
    }
}
