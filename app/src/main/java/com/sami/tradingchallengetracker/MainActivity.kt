package com.sami.tradingchallengetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sami.tradingchallengetracker.data.ChallengeEntity
import com.sami.tradingchallengetracker.data.MilestoneEntity
import com.sami.tradingchallengetracker.data.MilestoneStatus
import com.sami.tradingchallengetracker.data.TradeEntity
import com.sami.tradingchallengetracker.ui.*
import com.sami.tradingchallengetracker.util.Money

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TradingChallengeTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    TradingApp(viewModel)
                }
            }
        }
    }
}

enum class ScreenTab {
    DASHBOARD, HISTORY, ROADMAP, SETTINGS
}

@Composable
fun TradingApp(viewModel: MainViewModel) {
    val challenge by viewModel.challenge.collectAsState()
    val trades by viewModel.trades.collectAsState()
    val milestones by viewModel.milestones.collectAsState()

    var activeTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }
    var showAddTradeModal by remember { mutableStateOf(false) }
    var selectedTrade by remember { mutableStateOf<TradeEntity?>(null) }
    var selectedMilestone by remember { mutableStateOf<MilestoneEntity?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showStartDialog by remember { mutableStateOf(false) }
    var showNewChallengeDialog by remember { mutableStateOf(false) }
    var newCapitalInput by remember { mutableStateOf("500") }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0D131F),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == ScreenTab.DASHBOARD,
                    onClick = { activeTab = ScreenTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                    label = { Text("الرئيسية", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AmberAccent,
                        selectedTextColor = AmberAccent,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
                NavigationBarItem(
                    selected = activeTab == ScreenTab.HISTORY,
                    onClick = { activeTab = ScreenTab.HISTORY },
                    icon = { Icon(Icons.Default.Description, contentDescription = "سجل الصفقات") },
                    label = { Text("سجل الصفقات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AmberAccent,
                        selectedTextColor = AmberAccent,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
                NavigationBarItem(
                    selected = activeTab == ScreenTab.ROADMAP,
                    onClick = { activeTab = ScreenTab.ROADMAP },
                    icon = { Icon(Icons.Default.Map, contentDescription = "خريطة المحطات") },
                    label = { Text("خريطة المحطات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AmberAccent,
                        selectedTextColor = AmberAccent,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
                NavigationBarItem(
                    selected = activeTab == ScreenTab.SETTINGS,
                    onClick = { activeTab = ScreenTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
                    label = { Text("الإعدادات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AmberAccent,
                        selectedTextColor = AmberAccent,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
            }
        },
        containerColor = DarkBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBg)
        ) {
            val ch = challenge
            if (ch != null) {
                when (activeTab) {
                    ScreenTab.DASHBOARD -> DashboardContent(
                        challenge = ch,
                        onOpenSettings = { activeTab = ScreenTab.SETTINGS },
                        onOpenAddTrade = { showAddTradeModal = true },
                        onStartChallenge = { showStartDialog = true },
                        onNewChallenge = { showNewChallengeDialog = true }
                    )
                    ScreenTab.HISTORY -> HistoryContent(
                        trades = trades,
                        onSharePdf = { viewModel.sharePdfReport() },
                        onSelectTrade = { selectedTrade = it },
                        onOpenAddTrade = { showAddTradeModal = true }
                    )
                    ScreenTab.ROADMAP -> RoadmapContent(
                        milestones = milestones,
                        onSelectMilestone = { selectedMilestone = it }
                    )
                    ScreenTab.SETTINGS -> SettingsContent(
                        challenge = ch,
                        onSave = { name, initCap, target ->
                            viewModel.saveSettings(name, initCap, target)
                        },
                        onResetClick = { showResetDialog = true },
                        onNewChallengeClick = { showNewChallengeDialog = true }
                    )
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AmberAccent
                )
            }
        }
    }

    // Add Trade Modal Dialog
    if (showAddTradeModal && challenge != null) {
        var tradeInput by remember { mutableStateOf("") }
        var isLossSign by remember { mutableStateOf(false) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddTradeModal = false },
            title = {
                Text(
                    "إضافة صفقة جديدة (الصفقة #${challenge!!.tradeCount + 1})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "الرصيد الحالي: ${Money.format(challenge!!.currentBalanceCents)}",
                        fontWeight = FontWeight.SemiBold,
                        color = AmberAccent
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { isLossSign = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isLossSign) ProfitGreen else Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ربح (+)", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Button(
                            onClick = { isLossSign = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLossSign) LossRed else Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("خسارة (-)", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    OutlinedTextField(
                        value = tradeInput,
                        onValueChange = { tradeInput = it; errorMsg = null },
                        placeholder = { Text("أدخل القيمة (مثال: 20 أو 15.5)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (errorMsg != null) {
                        Text(errorMsg!!, color = LossRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val inputWithSign = if (isLossSign && !tradeInput.startsWith("-")) "-$tradeInput" else tradeInput
                        val parseResult = Money.parseToCents(inputWithSign)
                        if (parseResult.isSuccess) {
                            val cents = parseResult.getOrThrow()
                            viewModel.recordTrade(
                                resultCents = cents,
                                attachmentPath = null,
                                onSuccess = {
                                    showAddTradeModal = false
                                },
                                onError = { errorMsg = it }
                            )
                        } else {
                            errorMsg = parseResult.exceptionOrNull()?.message ?: "رقم غير صحيح"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                ) {
                    Text("تأكيد الصفقة", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTradeModal = false }) {
                    Text("إلغاء", color = Color.LightGray)
                }
            },
            containerColor = CardBg
        )
    }

    // Reset Confirmation
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("إعادة التحدي؟", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "سيؤدي هذا الإجراء إلى حذف سجل الصفقات وإعادة جميع المحطات إلى حالتها الأولية.\nهل أنت متأكد؟",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetChallenge { showResetDialog = false }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text("تأكيد", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("إلغاء", color = Color.LightGray)
                }
            },
            containerColor = CardBg
        )
    }

    // Start Challenge Confirmation
    if (showStartDialog && challenge != null) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text("تأكيد بدء التحدي", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    "سيتم بدء التحدي برأس مال ${Money.format(challenge!!.initialCapitalCents)}.\nهل أنت متأكد من بدء التحدي؟",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startChallenge()
                        showStartDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                ) {
                    Text("تأكيد", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) {
                    Text("إلغاء", color = Color.LightGray)
                }
            },
            containerColor = CardBg
        )
    }

    // New Challenge Dialog
    if (showNewChallengeDialog) {
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showNewChallengeDialog = false },
            title = { Text("بدء تحدٍ جديد", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل رأس المال الابتدائي للتحدي الجديد ($):", color = Color.LightGray)
                    OutlinedTextField(
                        value = newCapitalInput,
                        onValueChange = { newCapitalInput = it; error = null },
                        placeholder = { Text("500") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error != null) {
                        Text(error!!, color = LossRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = Money.parseCapitalToCents(newCapitalInput)
                        if (parsed.isSuccess) {
                            viewModel.startNewChallenge(parsed.getOrThrow()) {
                                showNewChallengeDialog = false
                            }
                        } else {
                            error = parsed.exceptionOrNull()?.message ?: "قيمة غير صحيحة"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                ) {
                    Text("بدء التحدي", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChallengeDialog = false }) {
                    Text("إلغاء", color = Color.LightGray)
                }
            },
            containerColor = CardBg
        )
    }
}

@Composable
fun DashboardContent(
    challenge: ChallengeEntity,
    onOpenSettings: () -> Unit,
    onOpenAddTrade: () -> Unit,
    onStartChallenge: () -> Unit,
    onNewChallenge: () -> Unit
) {
    val progress = Money.calculateProgress(challenge.currentBalanceCents, challenge.targetBalanceCents)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF1E293B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = AmberAccent)
                }
                Column {
                    Text("مرحباً بك", fontSize = 11.sp, color = TextMuted)
                    Text(
                        challenge.userName.ifEmpty { "المتداول" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "الإعدادات", tint = TextMuted)
            }
        }

        // Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("الرصيد الحالي", fontSize = 12.sp, color = TextMuted)
                    Text(
                        "البدء: ${Money.format(challenge.initialCapitalCents)}",
                        fontSize = 11.sp,
                        color = AmberAccent
                    )
                }
                Text(
                    Money.format(challenge.currentBalanceCents),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                // Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الهدف: ${Money.format(challenge.targetBalanceCents)}", fontSize = 12.sp, color = TextMuted)
                        Text("${String.format(java.util.Locale.US, "%.2f", progress)}%", fontSize = 12.sp, color = AmberAccent, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = AmberAccent,
                        trackColor = Color(0xFF1E293B),
                    )
                }
            }
        }

        // 4 Stat Cards
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                title = "رأس المال الابتدائي",
                value = Money.format(challenge.initialCapitalCents),
                sub = "نقطة الانطلاق",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "عدد الصفقات",
                value = "${challenge.tradeCount} / 150",
                sub = "متبقي ${150 - challenge.tradeCount}",
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                title = "الرصيد الحالي",
                value = Money.format(challenge.currentBalanceCents),
                sub = if (challenge.tradeCount == 0) "قبل البدء" else "رصيد الحساب",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "الهدف النهائي",
                value = Money.format(challenge.targetBalanceCents),
                sub = "خطة النهاية",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Actions
        if (!challenge.challengeStarted) {
            Button(
                onClick = onStartChallenge,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
            ) {
                Text("ابدأ التحدي برأس مال ${Money.format(challenge.initialCapitalCents)}", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onOpenAddTrade,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                enabled = challenge.tradeCount < 150
            ) {
                Text("إضافة صفقة جديدة (الصفقة #${challenge.tradeCount + 1})", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onNewChallenge,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("ابدأ تحدياً جديداً", color = AmberAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 11.sp, color = TextMuted)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(sub, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
fun HistoryContent(
    trades: List<TradeEntity>,
    onSharePdf: () -> Unit,
    onSelectTrade: (TradeEntity) -> Unit,
    onOpenAddTrade: () -> Unit
) {
    var filter by remember { mutableStateOf("all") }

    var totalProfits = 0L
    var totalLosses = 0L
    var netResult = 0L
    trades.forEach { t ->
        if (t.resultCents > 0) totalProfits += t.resultCents
        else if (t.resultCents < 0) totalLosses += Math.abs(t.resultCents)
        netResult += t.resultCents
    }

    val filtered = trades.filter {
        when (filter) {
            "win" -> it.type == com.sami.tradingchallengetracker.data.TradeType.WIN
            "loss" -> it.type == com.sami.tradingchallengetracker.data.TradeType.LOSS
            "image" -> it.attachmentPath != null
            else -> true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("سجل الصفقات", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            OutlinedButton(
                onClick = onSharePdf,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent)
            ) {
                Text("مشاركة التقرير (PDF)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 4 Metric Boxes
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.weight(1f).background(CardBg, RoundedCornerShape(12.dp)).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("صافي النتائج", fontSize = 9.sp, color = TextMuted)
                    Text(Money.format(netResult, true), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (netResult >= 0) ProfitGreen else LossRed)
                }
            }
            Box(
                modifier = Modifier.weight(1f).background(CardBg, RoundedCornerShape(12.dp)).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("إجمالي الخسائر", fontSize = 9.sp, color = TextMuted)
                    Text(if (totalLosses > 0) "-${Money.format(totalLosses)}" else "$0.00", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LossRed)
                }
            }
            Box(
                modifier = Modifier.weight(1f).background(CardBg, RoundedCornerShape(12.dp)).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("إجمالي الأرباح", fontSize = 9.sp, color = TextMuted)
                    Text(Money.format(totalProfits, true), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                }
            }
            Box(
                modifier = Modifier.weight(1f).background(CardBg, RoundedCornerShape(12.dp)).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("عدد الصفقات", fontSize = 9.sp, color = TextMuted)
                    Text("${trades.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Filter tabs
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChipItem("الكل", filter == "all") { filter = "all" }
            FilterChipItem("الربح ↑", filter == "win") { filter = "win" }
            FilterChipItem("الخسارة ↓", filter == "loss") { filter = "loss" }
            FilterChipItem("مع صورة 📷", filter == "image") { filter = "image" }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("لا توجد صفقات حتى الآن", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("ابدأ بإضافة أول صفقة لمتابعة تقدمك.", fontSize = 12.sp, color = TextMuted)
                    Button(
                        onClick = onOpenAddTrade,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                    ) {
                        Text("إضافة صفقة", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.id }) { trade ->
                    val isWin = trade.type == com.sami.tradingchallengetracker.data.TradeType.WIN
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelectTrade(trade) },
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("#${trade.tradeNumber}", fontWeight = FontWeight.Bold, color = Color.White)
                                    Box(
                                        modifier = Modifier
                                            .background(if (isWin) Color(0x2210B981) else Color(0x22EF4444), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(if (isWin) "✔ ربح" else "✕ خسارة", fontSize = 10.sp, color = if (isWin) ProfitGreen else LossRed, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(Money.formatDateTime(trade.timestamp), fontSize = 10.sp, color = TextMuted)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("نتيجة الصفقة", fontSize = 10.sp, color = TextMuted)
                                    Text(Money.format(trade.resultCents, true), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isWin) ProfitGreen else LossRed)
                                }
                                Column {
                                    Text("الرصيد السابق", fontSize = 10.sp, color = TextMuted)
                                    Text(Money.format(trade.oldBalanceCents), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                }
                                Column {
                                    Text("الرصيد الجديد", fontSize = 10.sp, color = TextMuted)
                                    Text(Money.format(trade.newBalanceCents), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.FilterChipItem(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .background(if (selected) AmberAccent else Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.Black else TextMuted)
    }
}

@Composable
fun RoadmapContent(
    milestones: List<MilestoneEntity>,
    onSelectMilestone: (MilestoneEntity) -> Unit
) {
    val completed = milestones.count { it.status != MilestoneStatus.PENDING }
    val remaining = 150 - completed

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("خريطة المحطات", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("جدول المحطات التفاعلي (150 محطة)", fontSize = 12.sp, color = TextMuted)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f).background(CardBg, RoundedCornerShape(12.dp)).padding(10.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المنجزة", fontSize = 11.sp, color = TextMuted)
                    Text("$completed", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                }
            }
            Box(modifier = Modifier.weight(1f).background(CardBg, RoundedCornerShape(12.dp)).padding(10.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المتبقية", fontSize = 11.sp, color = TextMuted)
                    Text("$remaining", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                }
            }
            Box(modifier = Modifier.weight(1f).background(CardBg, RoundedCornerShape(12.dp)).padding(10.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الإجمالي", fontSize = 11.sp, color = TextMuted)
                    Text("150", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Table Header
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1E293B), RoundedCornerShape(8.dp)).padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("حالة الصفقة", fontSize = 11.sp, color = TextMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("الربح", fontSize = 11.sp, color = TextMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("رصيد الحساب", fontSize = 11.sp, color = TextMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("المحطة", fontSize = 11.sp, color = TextMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            items(milestones, key = { it.id }) { m ->
                val isWin = m.status == MilestoneStatus.WIN
                val isLoss = m.status == MilestoneStatus.LOSS
                val isPending = m.status == MilestoneStatus.PENDING

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBg, RoundedCornerShape(8.dp))
                        .clickable { onSelectMilestone(m) }
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        when {
                            isWin -> Text("✔", color = ProfitGreen, fontWeight = FontWeight.Bold)
                            isLoss -> Text("✕", color = LossRed, fontWeight = FontWeight.Bold)
                            else -> Text("○", color = TextMuted)
                        }
                    }
                    Text(
                        Money.format(m.profitCents, true),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWin) ProfitGreen else if (isLoss) LossRed else TextMuted,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        Money.format(m.balanceCents),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPending) TextMuted else Color.White,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "#${m.milestoneNumber}",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsContent(
    challenge: ChallengeEntity,
    onSave: (String, Long, Long) -> Unit,
    onResetClick: () -> Unit,
    onNewChallengeClick: () -> Unit
) {
    var nameInput by remember { mutableStateOf(challenge.userName) }
    var capitalInput by remember { mutableStateOf((challenge.initialCapitalCents / 100L).toString()) }
    var targetInput by remember { mutableStateOf((challenge.targetBalanceCents / 100L).toString()) }
    var savedAlert by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("إعدادات التحدي", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("اسم المتداول", fontSize = 12.sp, color = TextMuted)
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text("مثال: Sami") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("رأس المال الابتدائي ($)", fontSize = 12.sp, color = TextMuted)
                OutlinedTextField(
                    value = capitalInput,
                    onValueChange = { capitalInput = it },
                    enabled = !challenge.challengeStarted,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("الهدف النهائي ($)", fontSize = 12.sp, color = TextMuted)
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val cap = Money.parseCapitalToCents(capitalInput).getOrDefault(challenge.initialCapitalCents)
                        val tar = Money.parseCapitalToCents(targetInput).getOrDefault(challenge.targetBalanceCents)
                        onSave(nameInput, cap, tar)
                        savedAlert = true
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberAccent)
                ) {
                    Text("حفظ الإعدادات", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                if (savedAlert) {
                    Text("تم حفظ الإعدادات بنجاح", color = ProfitGreen, fontSize = 12.sp)
                }
            }
        }

        Text("إدارة البيانات والتحدي", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMuted)

        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onResetClick),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("إعادة التحدي", fontWeight = FontWeight.Bold, color = LossRed)
                    Text("حذف سجل الصفقات وإعادة جميع المحطات إلى حالتها الأولية", fontSize = 11.sp, color = TextMuted)
                }
                Icon(Icons.Default.Refresh, contentDescription = null, tint = LossRed)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onNewChallengeClick),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ابدأ تحدياً جديداً", fontWeight = FontWeight.Bold, color = AmberAccent)
                    Text("إنشاء تحدٍ جديد برأس مال مختلف", fontSize = 11.sp, color = TextMuted)
                }
                Icon(Icons.Default.AddCircle, contentDescription = null, tint = AmberAccent)
            }
        }
    }
}
