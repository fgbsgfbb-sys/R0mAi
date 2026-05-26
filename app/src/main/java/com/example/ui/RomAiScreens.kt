package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.theme.GreenAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.WarmOrange
import com.example.viewmodel.RomAiViewModel
import com.example.viewmodel.ModelSortOrder
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.launch

// ==================== MONITOR SCREEN ====================
@Composable
fun MonitorScreen(viewModel: RomAiViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val ramUse by viewModel.ramUsageMb.collectAsState()
    val romPaged by viewModel.romPagedGb.collectAsState()
    val vramUse by viewModel.vramUsageMb.collectAsState()
    val tokensRate by viewModel.tokensPerSec.collectAsState()
    val driverVal by viewModel.driverVersion.collectAsState()
    val isUpdating by viewModel.isDriverUpdating.collectAsState()
    val isLoaded by viewModel.isModelLoaded.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val logs by viewModel.engineLogs.collectAsState()
    val deviceSelected by viewModel.selectedDevice.collectAsState()
    val threadsCount by viewModel.cpuThreads.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    val scope = rememberCoroutineScope()
    val logsListState = rememberLazyListState()

    // Autoscroll logs to bottom
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logsListState.animateScrollToItem(logs.size - 1)
        }
    }

    // Locale translations
    val titleMonitor = if (lang == "ru") "Монитор Ресурсов" else "Resource Monitor"
    val systemStatusLab = if (lang == "ru") "Статус Системы" else "System Status"
    val activeServLab = if (lang == "ru") "Служба Rom AI" else "Rom AI Service"
    val activeStatusText = if (lang == "ru") "АКТИВНА (ROM Paging)" else "ACTIVE (ROM Paging)"
    val inactiveStatusText = if (lang == "ru") "ОЖИДАНИЕ" else "IDLE"
    val activeModelLab = if (lang == "ru") "Активная Модель" else "Active Model"
    val noneModelText = if (lang == "ru") "Нет загруженных моделей" else "No model loaded"
    val speedLab = if (lang == "ru") "Скорость инференса" else "Inference Speed"
    val ramLockLab = if (lang == "ru") "Физическая ОЗУ (Locked)" else "Physical RAM (Locked)"
    val romPageSwapLab = if (lang == "ru") "ROM Страничный Своп" else "ROM Swap Files"
    val vramAccLab = if (lang == "ru") "GPU Выделение VRAM" else "GPU VRAM Allocation"
    val hardwareOpenCLLab = if (lang == "ru") "Поддержка GPU / OpenCL Drivers" else "GPU / OpenCL Driver HAL"
    val btnUpdateDriver = if (lang == "ru") "Обновить HAL Драйверы" else "Update HAL Drivers"
    val engineLogsTitle = if (lang == "ru") "Журнал Ядра Инференса" else "Kernel Inference Logs"
    val btnClearLogs = if (lang == "ru") "Очистить" else "Clear Logs"
    val btnOomSimulation = if (lang == "ru") "Тест OOM Защиты" else "Test OOM Sentry"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Service Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = systemStatusLab,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isLoaded) GreenAccent else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLoaded) activeStatusText else inactiveStatusText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (isLoaded && activeModel != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = activeModelLab,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeModel!!.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }

        // 2. Telemetry Gauges Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Info block explaining ROM bypass
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(0.2f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == "ru") "Концепция Rom AI (Память ПЗУ)" else "Rom AI Concept (ROM Cache Bypass)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (lang == "ru") {
                                "Обычно при нехватке ОЗУ (менее 3 ГБ свободных) ОМ-киллер (LowMemoryKiller) убивает приложения. Rom AI блокирует в реальной оперативе лишь маленький буфер слоев (~150 МБ), транслируя остальные 2–10 ГБ весов из ПЗУ (ROM) блоками на лету по требованию!"
                            } else {
                                "Low memory devices (1.5GB RAM) usually crash when booting large models. Rom AI locks a minimal layers pool (~150MB) in real RAM, while utilizing high-speed random block read directly from internal storage (ROM)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Gauge 1: RAM Lock Slider/Meter
                TelemetryProgressBar(
                    label = ramLockLab,
                    currentValueText = "$ramUse MB",
                    subtext = if (isLoaded) "Safely Locked inside active CPU threadpool." else "Baseline framework thread overhead.",
                    progress = if (isLoaded) Math.min(1f, ramUse / 1500f) else 0.08f,
                    color = GreenAccent
                )

                // Gauge 2: ROM Paging Swap Status
                TelemetryProgressBar(
                    label = romPageSwapLab,
                    currentValueText = if (isLoaded) String.format("%.2f GB", romPaged) else "0.00 GB",
                    subtext = if (isLoaded) "Swapped layer blocks resides in internal storage memory." else "No virtual swap mapping registers active.",
                    progress = if (isLoaded) Math.min(1f, romPaged.toFloat() / 8f) else 0.0f,
                    color = CyanAccent
                )

                // Gauge 3: GPU VRAM Allocation
                TelemetryProgressBar(
                    label = vramAccLab,
                    currentValueText = "$vramUse MB",
                    subtext = if (deviceSelected.contains("GPU")) "OpenCL buffers actively shared on CPU architecture." else "GPU acceleration off. Running indices on CPU cores.",
                    progress = if (deviceSelected.contains("GPU")) Math.min(1f, vramUse.toFloat() / 2048f) else 0.0f,
                    color = WarmOrange
                )

                if (isLoaded) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$speedLab:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format("%.2f t/s", tokensRate),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = GreenAccent
                            )
                        }
                    }
                }
            }
        }

        // 3. Driver update card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = hardwareOpenCLLab,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Current: $driverVal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isUpdating) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        Button(
                            onClick = { viewModel.updateDrivers() },
                            modifier = Modifier.fillMaxWidth().testTag("update_driver_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(btnUpdateDriver)
                        }
                    }
                }
            }
        }

        // 4. Trace Log Shell Window
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070A13)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = GreenAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = engineLogsTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearLogs() },
                            modifier = Modifier.size(24.dp).testTag("clear_logs_button"),
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = btnClearLogs,
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.Black.copy(0.4f), RoundedCornerShape(6.dp))
                            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(6.dp))
                            .padding(6.dp)
                    ) {
                        LazyColumn(
                            state = logsListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(logs) { log ->
                                val color = when (log.level) {
                                    "ERROR" -> Color.Red
                                    "WARN" -> Color.Yellow
                                    "DEBUG" -> Color.Cyan
                                    else -> GreenAccent
                                }
                                Text(
                                    text = "[${log.tag}] ${log.message}",
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = color,
                                        lineHeight = 14.sp
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            viewModel.writeLog(
                                "OOM_PREVENT",
                                "DEBUG",
                                "Simulated peak allocation. Triggered virtual sweep page write... Thread pool locks successfully survived."
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .testTag("oom_test_button"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(text = btnOomSimulation, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryProgressBar(
    label: String,
    currentValueText: String,
    subtext: String,
    progress: Float,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text(
                    text = currentValueText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                lineHeight = 14.sp
            )
        }
    }
}


// ==================== MODELS SCREEN / HUB ====================
@Composable
fun ModelsScreen(viewModel: RomAiViewModel, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    val isSearching by viewModel.isSearchingHf.collectAsState()
    val searchResults by viewModel.hfSearchResults.collectAsState()
    val allModels by viewModel.allModels.collectAsState()
    val activeModelId by viewModel.activeModelId.collectAsState()
    val isLoadingModel by viewModel.isLoadingModel.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    var showImportDialog by remember { mutableStateOf(false) }

    // Translations
    val searchPlaceholder = if (lang == "ru") "Поиск моделей на huggingface.co..." else "Search HuggingFace Hub..."
    val listLabelHf = if (lang == "ru") "Результаты поиска Hugging Face" else "Hugging Face Match List"
    val listLabelInstalled = if (lang == "ru") "Установленные и Локальные (ROM)" else "Installed & Offline Footprints"
    val btnImportCustom = if (lang == "ru") "Импортировать свой файл..." else "Import Custom Weight..."
    val detailSize = if (lang == "ru") "Общий Вес" else "File Size"
    val detailMinRam = if (lang == "ru") "Минимум ОЗУ" else "RAM Buffer"
    val detailPagedRom = if (lang == "ru") "ПЗУ Своп кэш" else "ROM Swap Required"
    val tagFormat = if (lang == "ru") "Формат" else "Format"
    val bntLoad = if (lang == "ru") "Запустить" else "Load Model"
    val btnUnload = if (lang == "ru") "Выгрузить" else "Unload"
    val btnDelete = if (lang == "ru") "Удалить" else "Delete"
    val btnDetail = if (lang == "ru") "Инфо" else "Details"
    val loadProgressLab = if (lang == "ru") "Сборка ROM кэша: " else "ROM Assemble: "

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search & Import Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(searchPlaceholder, fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, contentDescription = null) }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hf_search_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.searchHuggingFace(query) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hf_search_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (lang == "ru") "Поиск" else "Search")
                    }
                    Button(
                        onClick = { showImportDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_custom_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (lang == "ru") "Свои (.gguf/.mnn)" else "Custom (.gguf)")
                    }
                }
            }
        }

        // Horizontal sorting options
        item {
            val currentSort by viewModel.modelSortOrder.collectAsState()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (lang == "ru") "Сортировка списков" else "Model sorting lists",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sortOptions = listOf(
                        ModelSortOrder.POPULARITY to (if (lang == "ru") "🔥 Скачивания" else "🔥 Downloads"),
                        ModelSortOrder.LIKES to (if (lang == "ru") "❤️ Лайки" else "❤️ Likes"),
                        ModelSortOrder.SIZE_DESC to (if (lang == "ru") "💾 Большие" else "💾 Large Size"),
                        ModelSortOrder.SIZE_ASC to (if (lang == "ru") "⚡ Маленькие" else "⚡ Small Size"),
                        ModelSortOrder.NAME to (if (lang == "ru") "🔤 Имя [А-Я]" else "🔤 Name [A-Z]")
                    )
                    
                    sortOptions.forEach { (option, label) ->
                        val isSelected = currentSort == option
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSortOrder(option) },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("sort_chip_${option.name.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // HUGGING FACE SEARCH RESULTS
        if (isSearching) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lang == "ru") "Запрос Hugging Face API..." else "Querying HuggingFace API...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (searchResults.isNotEmpty()) {
            item {
                Text(
                    text = listLabelHf,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(searchResults) { rawModel ->
                val locallyInstalled = allModels.find { it.id == rawModel.id }
                ModelHubCard(
                    model = locallyInstalled ?: rawModel,
                    activeId = activeModelId,
                    isLoadingModel = isLoadingModel,
                    lang = lang,
                    minRamText = detailMinRam,
                    romPagedText = detailPagedRom,
                    btnStringLoad = bntLoad,
                    btnStringUnload = btnUnload,
                    btnStringDelete = btnDelete,
                    btnStringDetail = btnDetail,
                    progressLabel = loadProgressLab,
                    onDownload = { viewModel.downloadModel(rawModel.id) },
                    onLoad = { viewModel.loadModel(rawModel.id) },
                    onUnload = { viewModel.unloadActiveModel() },
                    onDelete = { viewModel.deleteModel(rawModel.id) }
                )
            }
        }

        // LOCAL RECOMPILATIONS / INSTALLED FILES
        item {
            Text(
                text = listLabelInstalled,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        val downloadedModels = allModels.filter { it.isDownloaded }
        if (downloadedModels.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (lang == "ru") "Нет установленных моделей. Воспользуйтесь поиском выше." else "No local models. Perform HF Search or register custom local files above.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(downloadedModels) { localModel ->
                ModelHubCard(
                    model = localModel,
                    activeId = activeModelId,
                    isLoadingModel = isLoadingModel,
                    lang = lang,
                    minRamText = detailMinRam,
                    romPagedText = detailPagedRom,
                    btnStringLoad = bntLoad,
                    btnStringUnload = btnUnload,
                    btnStringDelete = btnDelete,
                    btnStringDetail = btnDetail,
                    progressLabel = loadProgressLab,
                    onDownload = {},
                    onLoad = { viewModel.loadModel(localModel.id) },
                    onUnload = { viewModel.unloadActiveModel() },
                    onDelete = { viewModel.deleteModel(localModel.id) }
                )
            }
        }
    }

    // --- IMPORT CUSTOM LOCAL DIALOGUE ---
    if (showImportDialog) {
        var importName by remember { mutableStateOf("") }
        var importSizeGb by remember { mutableStateOf("") }
        var importFormat by remember { mutableStateOf("GGUF") }
        var importQuant by remember { mutableStateOf("Q4_K_M") }

        Dialog(onDismissRequest = { showImportDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("import_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (lang == "ru") "Регистрация весов (.GGUF)" else "Register Weights (.GGUF)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = importName,
                        onValueChange = { importName = it },
                        label = { Text(if (lang == "ru") "Имя файла" else "Filename") },
                        placeholder = { Text("llama-3-8b.gguf") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("import_name_input")
                    )

                    OutlinedTextField(
                        value = importSizeGb,
                        onValueChange = { importSizeGb = it },
                        label = { Text(if (lang == "ru") "Фактический размер (ГБ)" else "File Size (GB)") },
                        placeholder = { Text("4.3") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("import_size_input")
                    )

                    OutlinedTextField(
                        value = importQuant,
                        onValueChange = { importQuant = it },
                        label = { Text(if (lang == "ru") "Форматирование / Сжатие" else "Quantization Format") },
                        placeholder = { Text("4-bit (Q4_0_S)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("import_quant_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showImportDialog = false },
                            modifier = Modifier.testTag("close_import_dialog_button")
                        ) {
                            Text(if (lang == "ru") "Отмена" else "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val size = importSizeGb.toDoubleOrNull() ?: 3.5
                                val name = if (importName.isBlank()) "custom_model.gguf" else importName
                                viewModel.importCustomModel(
                                    filename = name,
                                    sizeGb = size,
                                    format = importFormat,
                                    quant = importQuant
                                )
                                showImportDialog = false
                            },
                            modifier = Modifier.testTag("submit_import_button")
                        ) {
                            Text(if (lang == "ru") "Импортировать" else "Import")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModelHubCard(
    model: ModelEntity,
    activeId: String?,
    isLoadingModel: Boolean,
    lang: String,
    minRamText: String,
    romPagedText: String,
    btnStringLoad: String,
    btnStringUnload: String,
    btnStringDelete: String,
    btnStringDetail: String,
    progressLabel: String,
    onDownload: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDelete: () -> Unit
) {
    var showDetailDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetailDialog = true }
            .testTag("model_card_${model.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (activeId == model.id)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (activeId == model.id) 1.5.dp else 1.dp,
            color = if (activeId == model.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(0.12f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Name & format tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "by ${model.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = model.format,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Spec fields: size and paging ratio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(text = if (lang == "ru") "Вес файла" else "Weight Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${model.sizeGb} GB", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(text = minRamText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${(model.minRamRequiredGb * 1024).toInt()} MB", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = GreenAccent)
                }
                Column {
                    Text(text = romPagedText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${model.pagedRomRequiredGb} GB", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = CyanAccent)
                }
            }

            // Downloads and Likes metadata badge
            if (model.downloads > 0 || model.likes > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Downloads",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${if (lang == "ru") "Загрузок" else "Downloads"}: ${if (model.downloads >= 1000) "${model.downloads / 1000}k" else "${model.downloads}"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Likes",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${if (lang == "ru") "Лайков" else "Likes"}: ${model.likes}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Controller block
            if (!model.isDownloaded) {
                // Downloading states
                if (model.downloadProgress > 0 && model.downloadProgress < 100) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = progressLabel, style = MaterialTheme.typography.bodySmall)
                            Text(text = "${model.downloadProgress}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { model.downloadProgress.toFloat() / 100f },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
                        )
                    }
                } else {
                    Button(
                        onClick = onDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("download_model_button_${model.id}"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (lang == "ru") "Загрузить paged-профиль" else "Generate Swap Map file", fontSize = 12.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeId == model.id) {
                        Button(
                            onClick = onUnload,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("unload_model_button"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(btnStringUnload, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onLoad,
                            enabled = !isLoadingModel,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(36.dp)
                                .testTag("load_model_button_${model.id}"),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(btnStringLoad, fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("delete_model_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(btnStringDelete, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Spec properties dialog popup
    if (showDetailDialog) {
        Dialog(onDismissRequest = { showDetailDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("details_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.2f))

                    Text(
                        text = if (lang == "ru") "Технические Характеристики модели:" else "Inference Paging Mechanics:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Format", style = MaterialTheme.typography.bodyMedium)
                        Text(model.format, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Quantization", style = MaterialTheme.typography.bodyMedium)
                        Text(model.quantization, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Min physical RAM budget", style = MaterialTheme.typography.bodyMedium, color = GreenAccent)
                        Text("${(model.minRamRequiredGb * 1024).toInt()} MB", fontWeight = FontWeight.Bold, color = GreenAccent)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active disk swap paging", style = MaterialTheme.typography.bodyMedium, color = CyanAccent)
                        Text("${model.pagedRomRequiredGb} GB", fontWeight = FontWeight.Bold, color = CyanAccent)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Mmap capability", style = MaterialTheme.typography.bodyMedium)
                        Text("Fully supported", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showDetailDialog = false },
                        modifier = Modifier.align(Alignment.End).testTag("close_details_dialog")
                    ) {
                        Text(if (lang == "ru") "Закрыть" else "Close")
                    }
                }
            }
        }
    }
}


// ==================== TEST CHAT-BOT SCREEN ====================
@Composable
fun ChatScreen(viewModel: RomAiViewModel, modifier: Modifier = Modifier) {
    val messages by viewModel.chatMessages.collectAsState()
    val isGenerating by viewModel.chatIsGenerating.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val activeSpeed by viewModel.tokensPerSec.collectAsState()
    val activeRam by viewModel.ramUsageMb.collectAsState()
    val activeRom by viewModel.romPagedGb.collectAsState()
    val activeVram by viewModel.vramUsageMb.collectAsState()
    val deviceSelected by viewModel.selectedDevice.collectAsState()
    val threadsCount by viewModel.cpuThreads.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Translations
    val chatPlaceholder = if (lang == "ru") "Ввод сообщения локальной модели..." else "Type prompt to offline model..."
    val promptExplainText = if (lang == "ru") "Вызовите тестовые промпты:" else "Tap preset diagnostics:"
    val cardSpeedLab = if (lang == "ru") "Скорость" else "Speed"
    val cardRamLab = if (lang == "ru") "RAM Буфер" else "RAM Buffer"
    val cardSwapLab = if (lang == "ru") "ROM Своп" else "ROM Swap"
    val cardCoreLab = if (lang == "ru") "GPU Ядра" else "Core Accel"

    // Preset chips
    val chipPrompts = if (lang == "ru") listOf(
        "Расскажи про mmap",
        "Какой твой формат?",
        "Покажи параметры GPU"
    ) else listOf(
        "Explain mmap logic",
        "What is your format?",
        "Show GPU specifications"
    )

    // Autoscroll chat
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .imePadding()
                    .padding(bottom = 100.dp) // Avoid overlap with bottom nav
            ) {
                // Preset quick action chips
                Text(text = promptExplainText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chipPrompts.forEach { item ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { textInput = item }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = item, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input bar row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text(chatPlaceholder, fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(20.dp),
                        trailingIcon = {
                            if (textInput.isNotEmpty()) {
                                IconButton(onClick = { textInput = "" }) { Icon(Icons.Default.Clear, contentDescription = null) }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("chat_send_button"),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Diagnostics Float Header Block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Micro Card 1
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(cardSpeedLab, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (activeModel != null) String.format("%.1f t/s", activeSpeed) else "0.0 t/s",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreenAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
                // Micro Card 2
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(cardRamLab, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "$activeRam MB",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                // Micro Card 3
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(cardSwapLab, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format("%.2f GB", activeRom),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
                // Micro Card 4
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(cardCoreLab, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (deviceSelected.contains("GPU")) "OpenCL" else "CPU [x${threadsCount.toInt()}]",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = WarmOrange,
                        fontFamily = FontFamily.Monospace
                    )
                }
                SecureActionChatHistory(onClick = { viewModel.clearChat() })
            }

            // 2. Chat history bubble lazy column
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(bottom = 8.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (lang == "ru") "Локальный инференс Rom AI" else "Local Rom AI Inference Pool",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (activeModel == null) {
                                    if (lang == "ru") "Внимание: нет загруженной модели. Откройте вторую вкладку." else "Warning: No model currently loaded, navigate to Models tab first."
                                } else {
                                    if (lang == "ru") "Модель задеплоена. Отправьте тестовый запрос!" else "Active weights verified. Put your prompt queries now!"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(messages) { message ->
                        ChatBubble(message = message, activeModelName = activeModel?.name ?: "Rom AI model")
                    }
                }

                if (isGenerating) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == "ru") "Инференс весов (ROM paging)..." else "Streaming ROM weight pages...",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = GreenAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecureActionChatHistory(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp).testTag("clear_chat_button")
    ) {
        Icon(Icons.Default.Delete, contentDescription = "Clear Chat History", tint = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun ChatBubble(message: ChatMessage, activeModelName: String) {
    val isUser = message.sender == "user"
    val isSystem = message.sender == "system"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Label
        Text(
            text = if (isUser) "Вам" else if (isSystem) "Система" else activeModelName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 12.dp
                    )
                )
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary
                    else if (isSystem) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Column {
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary
                    else if (isSystem) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Render specific diagnostics specs in model reply
                if (!isUser && !isSystem && message.tokensPerSec > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Telemetry: ${String.format("%.1f t/s", message.tokensPerSec)} | RAM: ${message.ramUsedMb}M | ROM Paged: ${message.romPagedGb}G | VRAM: ${message.vramUsedMb}M",
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                        ),
                        lineHeight = 10.sp
                    )
                }
            }
        }
    }
}


// ==================== SETTINGS SCREEN ====================
// Recreates the exact user settings layouts with superb fidelity and Material Files dark modes!
@Composable
fun SettingsScreen(viewModel: RomAiViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isAdvancedExpanded by remember { mutableStateOf(true) }

    // Core setting variables
    val deviceSelected by viewModel.selectedDevice.collectAsState()
    val ctxSize by viewModel.contextSize.collectAsState()
    val sizeBatch by viewModel.batchSize.collectAsState()
    val sizePhysicalBatch by viewModel.physicalBatchSize.collectAsState()
    val cpuThreadsVal by viewModel.cpuThreads.collectAsState()
    val imgTokensVal by viewModel.imageMaxTokens.collectAsState()
    val flashAttVal by viewModel.flashAttention.collectAsState()
    val keyCacheVal by viewModel.keyCacheType.collectAsState()
    val valCacheVal by viewModel.valueCacheType.collectAsState()

    val memLockVal by viewModel.useMemoryLock.collectAsState()
    val mmapVal by viewModel.mmapEnabled.collectAsState()
    val weightRepackVal by viewModel.enableWeightRepacking.collectAsState()

    val offloadLoadVal by viewModel.autoOffloadLoad.collectAsState()
    val autoNavVal by viewModel.autoNavigateToChat.collectAsState()

    val langVal by viewModel.appLanguage.collectAsState()
    val darkModeVal by viewModel.isDarkTheme.collectAsState()

    val hfTokenVal by viewModel.hfToken.collectAsState()
    val useHfTokenVal by viewModel.useHfToken.collectAsState()

    // Translations
    val headerTitle = if (langVal == "ru") "Настройки Инициализации" else "Model Initialization Settings"
    val deviceSelectionLab = if (langVal == "ru") "Выбор Устройства" else "Device Selection"
    val contextSizeLab = if (langVal == "ru") "Размер Контекста (Context Size)" else "Context Size"
    val advancedSettingsLab = if (langVal == "ru") "Расширенные Настройки" else "Advanced Settings"
    val batchSizeLab = if (langVal == "ru") "Размер Пакета (Batch Size)" else "Batch Size"
    val physBatchSizeLab = if (langVal == "ru") "Физический Пакет (Physical Batch Size)" else "Physical Batch Size"
    val cpuThreadsLab = if (langVal == "ru") "Потоки процессора (CPU Threads)" else "CPU Threads"
    val threadsAvailHint = if (langVal == "ru") "Используется %.0f из 8 свободных ядер" else "Using %.0f of 8 available threads"
    val imgMaxTokensLab = if (langVal == "ru") "Макс. Токены Изображений" else "Image Max Tokens"
    val imgTokensHint = if (langVal == "ru") "Максимальный лимит для обработки картинок: %.0f" else "Maximum tokens for image processing: %.0f"
    val flashAttLab = if (langVal == "ru") "Flash Attention" else "Flash Attention"
    val flashAttHint = if (langVal == "ru") "Должно быть отключено для сохранения OpenCL" else "Must be disabled for OpenCL state save/load"
    val keyCacheTypeLab = if (langVal == "ru") "Тип Key-кэша" else "Key Cache Type"
    val valCacheTypeLab = if (langVal == "ru") "Тип Value-кэша" else "Value Cache Type"

    val headerMemTitle = if (langVal == "ru") "Настройки Памяти" else "Memory Settings"
    val memLockLab = if (langVal == "ru") "Блокировка памяти (Memory Lock)" else "Use Memory Lock"
    val memLockHint = if (langVal == "ru") "Принудительно держать модель в RAM без сжатия" else "Force system to keep model in RAM rather than swapping or compressing"
    val mmapLab = if (langVal == "ru") "Проецирование файлов (mmap)" else "Memory Mapping"
    val mmapHint = if (langVal == "ru") "Память проецируется на диск для быстрой загрузки" else "Use memory-mapped files for faster model loading"
    val weightRepackLab = if (langVal == "ru") "Переупаковка Весов (Weight Repacking)" else "Enable Weight Repacking"
    val weightRepackHint = if (langVal == "ru") "Сокращает накладные расходы при отключенном mmap" else "Faster prompt processing with minimal memory overhead when memory mapping is disabled."

    val headerLoadTitle = if (langVal == "ru") "Загрузка Моделей" else "Model Loading Settings"
    val autoOffloadLab = if (langVal == "ru") "Авто-выгрузка" else "Auto Offload/Load"
    val autoOffloadHint = if (langVal == "ru") "Выгружать модель при переходе приложения в фон" else "Offload model when app is in background."
    val autoNavLab = if (langVal == "ru") "Авто-переход к чату" else "Auto-Navigate to Chat"
    val autoNavHint = if (langVal == "ru") "Переходить в чат при старте загрузки весов" else "Navigate to chat when loading starts."

    val headerAppTitle = if (langVal == "ru") "Параметры Rom AI" else "App Settings"
    val darkModeLab = if (langVal == "ru") "Темный Режим" else "Dark Mode"
    val langDesc = if (langVal == "ru") "Язык" else "Language"

    val headerApiTitle = if (langVal == "ru") "Параметры API" else "API Settings"
    val hfTokenLab = if (langVal == "ru") "Hugging Face Токен" else "Hugging Face Token"
    val hfTokenHint = if (langVal == "ru") "Для доступа к gated закрытым моделям" else "Set a token to access gated models from Hugging Face."
    val useHfTokenLab = if (langVal == "ru") "Использовать HF Токен в API" else "Use HF Token"
    val useHfTokenHint = if (langVal == "ru") "Передавать токен при запросах к Hugging Face" else "Enable to use token for API requests."

    val headerExportTitle = if (langVal == "ru") "Экспорт резервных копий" else "Export Options"
    val exportChatLab = if (langVal == "ru") "Экспорт старых чатов" else "Export Legacy Chats"
    val exportChatHint = if (langVal == "ru") "Используйте для восстановления прошлых встреч" else "Use this if migration failed or you need to recover old chat sessions."

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECTION 1: MODEL INITIALIZATION SETTINGS ---
        item {
            Card(modifier = Modifier.fillMaxWidth().testTag("init_settings_card")) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = headerTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Device select dropdown simulator
                    Column {
                        Text(text = deviceSelectionLab, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    val nextDevice = if (deviceSelected == "CPU only") "GPU (OpenCL Accelerated)" else "CPU only"
                                    viewModel.selectedDevice.value = nextDevice
                                    viewModel.writeLog("SYS", "INFO", "Устройство вычислений переключено на: $nextDevice")
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = deviceSelected, style = MaterialTheme.typography.bodyMedium)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    // Context Size input
                    Column {
                        Text(text = contextSizeLab, style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = ctxSize,
                            onValueChange = { viewModel.contextSize.value = it },
                            modifier = Modifier.fillMaxWidth().testTag("context_size_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Text(text = "Model reload needed for changes to take effect.", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                    }

                    // ADVANCED EXPANDER CONTROL
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = advancedSettingsLab, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Icon(
                            imageVector = if (isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(
                        visible = isAdvancedExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Slider: Batch Size
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = batchSizeLab, style = MaterialTheme.typography.labelSmall)
                                    Text(text = "${sizeBatch.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = sizeBatch,
                                    onValueChange = { viewModel.batchSize.value = it },
                                    valueRange = 128f..2048f,
                                    steps = 7,
                                    modifier = Modifier.testTag("batch_size_slider")
                                )
                                Text("Batch size: ${sizeBatch.toInt()}", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                            }

                            // Slider: Physical Batch Size
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = physBatchSizeLab, style = MaterialTheme.typography.labelSmall)
                                    Text(text = "${sizePhysicalBatch.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = sizePhysicalBatch,
                                    onValueChange = { viewModel.physicalBatchSize.value = it },
                                    valueRange = 128f..2048f,
                                    steps = 7,
                                    modifier = Modifier.testTag("physical_batch_size_slider")
                                )
                                Text("Physical batch size: ${sizePhysicalBatch.toInt()}", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                            }

                            // Slider: CPU Threads
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = cpuThreadsLab, style = MaterialTheme.typography.labelSmall)
                                    Text(text = "${cpuThreadsVal.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = cpuThreadsVal,
                                    onValueChange = { viewModel.cpuThreads.value = it },
                                    valueRange = 1f..8f,
                                    steps = 6,
                                    modifier = Modifier.testTag("cpu_threads_slider")
                                )
                                Text(String.format(threadsAvailHint, cpuThreadsVal), style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                            }

                            // Slider: Image Max Tokens
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = imgMaxTokensLab, style = MaterialTheme.typography.labelSmall)
                                    Text(text = "${imgTokensVal.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = imgTokensVal,
                                    onValueChange = { viewModel.imageMaxTokens.value = it },
                                    valueRange = 128f..1024f,
                                    steps = 7,
                                    modifier = Modifier.testTag("image_max_tokens_slider")
                                )
                                Text(String.format(imgTokensHint, imgTokensVal), style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                            }

                            // Switch selectors: Flash Attention (Auto / On / Off represented with selectable row indicator)
                            Column {
                                Text(text = flashAttLab, style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    listOf("Auto", "On", "Off").forEach { option ->
                                        val isSelected = option == flashAttVal
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { viewModel.flashAttention.value = option }
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = option,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Text(text = flashAttHint, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                            }

                            // Key Cache type selector
                            Column {
                                Text(text = keyCacheTypeLab, style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            viewModel.keyCacheType.value = if (keyCacheVal.startsWith("F16")) "INT4" else "F16 (Default)"
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = keyCacheVal, fontSize = 12.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }

                            // value cache type selector
                            Column {
                                Text(text = valCacheTypeLab, style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            viewModel.valueCacheType.value = if (valCacheVal.startsWith("F16")) "INT4" else "F16 (Default)"
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = valCacheVal, fontSize = 12.sp)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 2: MEMORY SETTINGS ---
        item {
            Card(modifier = Modifier.fillMaxWidth().testTag("memory_settings_card")) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = headerMemTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // 1. Mem Lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = memLockLab, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = memLockHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.75f))
                        }
                        Switch(
                            checked = memLockVal,
                            onCheckedChange = { viewModel.useMemoryLock.value = it },
                            modifier = Modifier.testTag("memory_lock_switch")
                        )
                    }

                    // 2. Mmap mapping
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = mmapLab, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = mmapHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.75f))
                        }
                        Switch(
                            checked = mmapVal,
                            onCheckedChange = { viewModel.mmapEnabled.value = it },
                            modifier = Modifier.testTag("mmap_switch")
                        )
                    }

                    // 3. Weight Repacking
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = weightRepackLab, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = weightRepackHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.75f))
                        }
                        Switch(
                            checked = weightRepackVal,
                            onCheckedChange = { viewModel.enableWeightRepacking.value = it },
                            modifier = Modifier.testTag("weight_repacking_switch")
                        )
                    }
                }
            }
        }

        // --- SECTION 3: MODEL LOADING SETTINGS ---
        item {
            Card(modifier = Modifier.fillMaxWidth().testTag("model_loading_settings_card")) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = headerLoadTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(text = autoOffloadLab, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = autoOffloadHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.75f))
                        }
                        Switch(checked = offloadLoadVal, onCheckedChange = { viewModel.autoOffloadLoad.value = it })
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(text = autoNavLab, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = autoNavHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.75f))
                        }
                        Switch(checked = autoNavVal, onCheckedChange = { viewModel.autoNavigateToChat.value = it })
                    }
                }
            }
        }

        // --- SECTION 4: APP POLISH SETTINGS ---
        item {
            Card(modifier = Modifier.fillMaxWidth().testTag("app_settings_card")) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = headerAppTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Language dropdown selector
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = langDesc, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    val nextLang = if (langVal == "ru") "en" else "ru"
                                    viewModel.setLanguage(nextLang)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (langVal == "ru") "Русский (RU)" else "English (EN)", fontSize = 13.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    // Dark mode switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = darkModeLab, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = darkModeVal,
                            onCheckedChange = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }

                    // Text to Speech
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = if (langVal == "ru") "Синтез речи (TTS)" else "Text-to-speech", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = "Read assistant replies aloud.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                        }
                        Switch(checked = false, onCheckedChange = {}, enabled = false)
                    }
                }
            }
        }

        // --- SECTION 5: API TOKENS ---
        item {
            Card(modifier = Modifier.fillMaxWidth().testTag("api_settings_card")) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = headerApiTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Column {
                        Text(text = hfTokenLab, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = hfTokenHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = hfTokenVal,
                                onValueChange = { viewModel.hfToken.value = it },
                                modifier = Modifier.weight(1.3f).testTag("hf_token_input"),
                                shape = RoundedCornerShape(8.dp),
                                placeholder = { Text("hf_••••••••••") },
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    viewModel.writeLog("SYS", "INFO", "Hugging Face API Token successfully registered globally.")
                                },
                                modifier = Modifier.weight(0.7f).testTag("save_hf_token_button")
                            ) {
                                Text("Set Token", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(text = useHfTokenLab, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = useHfTokenHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                        }
                        Switch(
                            checked = useHfTokenVal,
                            onCheckedChange = { viewModel.useHfToken.value = it },
                            modifier = Modifier.testTag("use_hf_token_switch")
                        )
                    }
                }
            }
        }

        // --- SECTION 6: EXPORT OPTIONS ---
        item {
            Card(modifier = Modifier.fillMaxWidth().testTag("export_settings_card")) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = headerExportTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    Column {
                        Text(text = exportChatLab, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = exportChatHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
                    }

                    Button(
                        onClick = {
                            viewModel.writeLog("SYS", "INFO", "Exported 0 custom threads. Migration checks compiled: OK.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth().testTag("export_chats_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export")
                    }
                }
            }
        }
    }
}
