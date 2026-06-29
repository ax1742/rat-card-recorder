package dm.app.card.fuck.df

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import dm.app.card.fuck.df.data.CardDatabaseHelper
import dm.app.card.fuck.df.ui.theme.CardCounterTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CardCounterTheme {
                CardCounterApp()
            }
        }
    }
}

data class CardType(
    val rank: String,
    val displayName: String,
    val suits: List<String>,
    val isJoker: Boolean = false
)

val cardTypes = listOf(
    CardType("A", "A", listOf("spade", "heart", "diamond", "club")),
    CardType("2", "2", listOf("spade", "heart", "diamond", "club")),
    CardType("3", "3", listOf("spade", "heart", "diamond", "club")),
    CardType("4", "4", listOf("spade", "heart", "diamond", "club")),
    CardType("5", "5", listOf("spade", "heart", "diamond", "club")),
    CardType("6", "6", listOf("spade", "heart", "diamond", "club")),
    CardType("7", "7", listOf("spade", "heart", "diamond", "club")),
    CardType("8", "8", listOf("spade", "heart", "diamond", "club")),
    CardType("9", "9", listOf("spade", "heart", "diamond", "club")),
    CardType("10", "10", listOf("spade", "heart", "diamond", "club")),
    CardType("J", "J", listOf("spade", "heart", "diamond", "club")),
    CardType("Q", "Q", listOf("spade", "heart", "diamond", "club")),
    CardType("K", "K", listOf("spade", "heart", "diamond", "club")),
    CardType("JOKER", "王", listOf("big_joker", "small_joker"), isJoker = true),
)

fun suitSymbol(suit: String): String = when (suit) {
    "spade" -> "♠"
    "heart" -> "♥"
    "diamond" -> "♦"
    "club" -> "♣"
    "big_joker" -> "大"
    "small_joker" -> "小"
    else -> "?"
}

fun suitColor(suit: String): Color = when (suit) {
    "heart", "diamond", "big_joker" -> Color(0xFFD32F2F)
    "spade", "club" -> Color(0xFF212121)
    "small_joker" -> Color(0xFF1565C0)
    else -> Color.Gray
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardCounterApp() {
    val context = LocalContext.current
    val db = remember { CardDatabaseHelper(context) }

    var collectedMap by remember { mutableStateOf<Map<Pair<String, String>, Boolean>>(emptyMap()) }
    var collectedCount by remember { mutableIntStateOf(0) }
    var showEasterEgg by remember { mutableStateOf(false) }
    var easterEggText by remember { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(false) }
    var pendingChanges by remember { mutableStateOf<Map<Pair<String, String>, Boolean>>(emptyMap()) }
    var showTutorial by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("card_counter_prefs", Context.MODE_PRIVATE) }

    fun loadData() {
        collectedMap = db.getAllCollected()
        collectedCount = db.getCollectedCount()
        pendingChanges = emptyMap()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val startTime = prefs.getLong("start_time", 0L).takeIf { it > 0 }
        val json = db.exportToJson(startTime)
        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
        Toast.makeText(context, "导出成功", Toast.LENGTH_SHORT).show()
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val json = context.contentResolver.openInputStream(uri)?.use { String(it.readBytes()) }
        if (json == null) {
            Toast.makeText(context, "读取文件失败", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        try {
            val importedStartTime = db.importFromJson(json)
            if (importedStartTime != null) {
                prefs.edit().putLong("start_time", importedStartTime).apply()
            }
            loadData()
            Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "导入失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleSuit(rank: String, suit: String) {
        if (!isEditMode) return
        val key = Pair(rank, suit)
        val current = pendingChanges[key] ?: collectedMap[key] ?: false
        pendingChanges = pendingChanges.toMutableMap().apply {
            put(key, !current)
        }
    }

    fun saveChanges() {
        if (pendingChanges.isEmpty()) return

        pendingChanges.forEach { (key, newValue) ->
            db.setCollected(key.first, key.second, newValue)
        }

        val currentCount = db.getCollectedCount()
        if (currentCount > 0 && !prefs.contains("start_time")) {
            prefs.edit().putLong("start_time", System.currentTimeMillis()).apply()
        }

        loadData()
        isEditMode = false
        Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()

        if (db.isAllCollected()) {
            val startTime = prefs.getLong("start_time", System.currentTimeMillis())
            val endTime = System.currentTimeMillis()
            val days = TimeUnit.MILLISECONDS.toDays(endTime - startTime)
            easterEggText = if (days == 0L) {
                "恭喜你！\n全部54张卡牌已收集完成！\n\n你仅用了不到1天就完成了！\n太厉害了！"
            } else {
                "恭喜你！\n全部54张卡牌已收集完成！\n\n你一共用了 ${days} 天\n从开始收集到全部完成！"
            }
            showEasterEgg = true
        }
    }

    fun cancelEdit() {
        pendingChanges = emptyMap()
        isEditMode = false
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { loadData() }
        val hasSeenTutorial = prefs.getBoolean("has_seen_tutorial", false)
        if (!hasSeenTutorial) {
            showTutorial = true
            prefs.edit().putBoolean("has_seen_tutorial", true).apply()
        }
    }

    val displayMap = collectedMap.toMutableMap().apply { putAll(pendingChanges) }
    val displayCount = displayMap.values.count { it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "鼠鼠卡牌收集记录器",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "已收集 $displayCount / 54 张",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (isEditMode) {
                        TextButton(onClick = { cancelEdit() }) {
                            Text("取消")
                        }
                        TextButton(onClick = { saveChanges() }) {
                            Text("保存")
                        }
                    } else {
                        TextButton(onClick = { showSettings = true }) {
                            Text("设置")
                        }
                        TextButton(onClick = { isEditMode = true }) {
                            Text("编辑")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            LinearProgressIndicator(
                progress = { displayCount / 54f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (displayCount == 54) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )

            Spacer(Modifier.height(4.dp))

            if (isEditMode) {
                Text(
                    text = "编辑模式 - 点击花色切换状态",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SuitLegend("♠", "黑桃", Color(0xFF1B1B1B))
                SuitLegend("♥", "红桃", Color(0xFFD32F2F))
                SuitLegend("♦", "方块", Color(0xFFD32F2F))
                SuitLegend("♣", "梅花", Color(0xFF1B1B1B))
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(cardTypes) { cardType ->
                    CardTypeItem(
                        cardType = cardType,
                        displayMap = displayMap,
                        isEditMode = isEditMode,
                        onToggle = { suit -> toggleSuit(cardType.rank, suit) },
                    )
                }
            }
        }
    }

    if (showEasterEgg) {
        EasterEggDialog(
            text = easterEggText,
            onDismiss = { showEasterEgg = false },
        )
    }

    if (showTutorial) {
        TutorialDialog(onDismiss = { showTutorial = false })
    }

    if (showSettings) {
        SettingsScreen(
            onDismiss = { showSettings = false },
            onImport = { importLauncher.launch(arrayOf("application/json")) },
            onExport = { exportLauncher.launch("card_collection.json") },
            onShowTutorial = { showSettings = false; showTutorial = true },
        )
    }
}

@Composable
fun SuitLegend(symbol: String, name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = symbol,
            fontSize = 14.sp,
            color = color,
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun CardTypeItem(
    cardType: CardType,
    displayMap: Map<Pair<String, String>, Boolean>,
    isEditMode: Boolean,
    onToggle: (String) -> Unit,
) {
    val allCollected = cardType.suits.all { suit ->
        displayMap[Pair(cardType.rank, suit)] ?: false
    }

    val borderColor = when {
        allCollected -> Color(0xFF4CAF50)
        isEditMode -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = when {
        allCollected -> 2.5.dp
        isEditMode -> 1.5.dp
        else -> 1.dp
    }
    val bgColor = when {
        allCollected -> Color(0xFF4CAF50).copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        tonalElevation = if (allCollected) 2.dp else 1.dp,
        modifier = Modifier.border(borderWidth, borderColor, RoundedCornerShape(10.dp)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = cardType.displayName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (allCollected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Always render 4 slots for consistent layout
                val displaySuits = if (cardType.isJoker) {
                    cardType.suits + listOf(null, null) // pad to 4
                } else {
                    cardType.suits
                }
                displaySuits.forEach { suit ->
                    if (suit != null) {
                        val key = Pair(cardType.rank, suit)
                        val collected = displayMap[key] ?: false
                        SuitBox(
                            suit = suit,
                            collected = collected,
                            isEditMode = isEditMode,
                            onClick = { onToggle(suit) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun SuitBox(
    suit: String,
    collected: Boolean,
    isEditMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = suitColor(suit)
    val symbol = suitSymbol(suit)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (collected) color.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceContainerLowest
            )
            .border(
                width = if (collected) 2.dp else 1.dp,
                color = if (collected) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp),
            )
            .clickable(enabled = isEditMode) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = symbol,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (collected) color else color.copy(alpha = 0.3f),
            )
            if (collected) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
    }
}

@Composable
fun EasterEggDialog(text: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "\uD83C\uDF89",
                    fontSize = 48.sp,
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = onDismiss) {
                    Text("太棒了！")
                }
            }
        }
    }
}

@Composable
fun TutorialDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "\uD83C\uDCCF",
                    fontSize = 48.sp,
                )
                Text(
                    text = "新手教程",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "点击软件右上角编辑，\n点击对应扑克牌的花色即可记录。",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = onDismiss) {
                    Text("我知道了")
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onShowTutorial: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "数据管理",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            onImport()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("导入数据")
                    }
                    FilledTonalButton(
                        onClick = {
                            onExport()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("导出数据")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "帮助",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedButton(
                    onClick = {
                        onShowTutorial()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("查看新手教程")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "关于 App",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "作者",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "大美·格里尔斯",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "邮箱",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "alexvictor17427@gmail.com",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("关闭")
                }
            }
        }
    }
}
