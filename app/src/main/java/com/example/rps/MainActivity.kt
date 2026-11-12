package com.example.rps

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Gold = Color(0xFFFFC107)

enum class Move(val emoji: String, val label: String) {
    ROCK("✊", "אבן"),
    PAPER("✋", "נייר"),
    SCISSORS("✌️", "מספריים");

    fun beats(other: Move) =
        (this == ROCK && other == SCISSORS) ||
        (this == PAPER && other == ROCK) ||
        (this == SCISSORS && other == PAPER)
}

enum class Outcome(val text: String, val color: Color) {
    WIN("ניצחת! 🎉", Color(0xFF69F0AE)),
    LOSE("הפסדת 😅", Color(0xFFFF8A80)),
    DRAW("תיקו 🤝", Color(0xFFCFD8DC))
}

enum class Phase { IDLE, SHAKING, REVEALED }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme { GameScreen() }
            }
        }
    }
}

@Composable
fun GameScreen() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("rps", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    var phase by rememberSaveable { mutableStateOf(Phase.IDLE) }
    var player by rememberSaveable { mutableStateOf<Move?>(null) }
    var computer by rememberSaveable { mutableStateOf<Move?>(null) }
    var outcome by rememberSaveable { mutableStateOf<Outcome?>(null) }
    var countdown by rememberSaveable { mutableStateOf("") }
    var wins by rememberSaveable { mutableIntStateOf(0) }
    var losses by rememberSaveable { mutableIntStateOf(0) }
    var draws by rememberSaveable { mutableIntStateOf(0) }
    var streak by rememberSaveable { mutableIntStateOf(0) }
    var reward by rememberSaveable { mutableIntStateOf(0) }
    var rain by rememberSaveable { mutableIntStateOf(0) }
    var coins by remember { mutableIntStateOf(prefs.getInt("coins", 0)) }

    LaunchedEffect(Unit) { if (phase == Phase.SHAKING) phase = Phase.IDLE }

    fun play(move: Move) {
        if (phase == Phase.SHAKING) return
        scope.launch {
            player = move
            computer = null
            outcome = null
            reward = 0
            phase = Phase.SHAKING
            for (word in listOf("אבן...", "נייר...", "מספריים!")) {
                countdown = word
                delay(430)
            }
            val cpu = Move.values().random()
            val result = when {
                move == cpu -> Outcome.DRAW
                move.beats(cpu) -> Outcome.WIN
                else -> Outcome.LOSE
            }
            computer = cpu
            outcome = result
            phase = Phase.REVEALED
            when (result) {
                Outcome.WIN -> {
                    wins++
                    streak++
                    reward = 10 + if (streak >= 3) 5 else 0
                    coins += reward
                    rain++
                }
                Outcome.LOSE -> {
                    losses++
                    streak = 0
                }
                Outcome.DRAW -> {
                    draws++
                    reward = 2
                    coins += reward
                }
            }
            prefs.edit().putInt("coins", coins).apply()
        }
    }

    fun highlight(isPlayer: Boolean): Int = when {
        phase != Phase.REVEALED -> 0
        outcome == Outcome.WIN -> if (isPlayer) 1 else -1
        outcome == Outcome.LOSE -> if (isPlayer) -1 else 1
        else -> 0
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1040), Color(0xFF3B1E6B), Color(0xFF5B2A86))
                )
            )
    ) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("אבן נייר ומספריים", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x33FFFFFF))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Coin(26.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("$coins", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ScoreCard("ניצחונות", wins, Outcome.WIN.color)
                ScoreCard("תיקו", draws, Outcome.DRAW.color)
                ScoreCard("הפסדים", losses, Outcome.LOSE.color)
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HandView("אתה", player, phase, false, highlight(true))
                Text("VS", color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                HandView("המחשב", computer, phase, true, highlight(false))
            }

            Column(
                Modifier.height(96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val statusText = when (phase) {
                    Phase.IDLE -> "בחר מהלך!"
                    Phase.SHAKING -> countdown
                    Phase.REVEALED -> outcome?.text ?: ""
                }
                Text(
                    statusText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (phase == Phase.REVEALED) (outcome?.color ?: Color.White) else Color.White,
                    textAlign = TextAlign.Center
                )
                if (phase == Phase.REVEALED && reward > 0) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Coin(24.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("+$reward", color = Gold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        if (outcome == Outcome.WIN && streak >= 2) {
                            Spacer(Modifier.width(10.dp))
                            Text("🔥 רצף $streak", color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Move.values().forEach { m ->
                        Column(
                            Modifier
                                .size(width = 96.dp, height = 104.dp)
                                .alpha(if (phase == Phase.SHAKING) 0.5f else 1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF7E57C2), Color(0xFF4527A0))
                                    )
                                )
                                .border(2.dp, Color(0x66FFFFFF), RoundedCornerShape(24.dp))
                                .clickable(enabled = phase != Phase.SHAKING) { play(m) }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(m.emoji, fontSize = 38.sp)
                            Text(m.label, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = {
                    wins = 0; losses = 0; draws = 0; streak = 0; reward = 0
                    player = null; computer = null; outcome = null
                    phase = Phase.IDLE
                }) { Text("אפס ניקוד", color = Color(0xCCFFFFFF)) }
            }
        }

        CoinRain(rain)
    }
}

@Composable
fun ScoreCard(label: String, value: Int, color: Color) {
    Column(
        Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x22FFFFFF))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$value", color = color, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xCCFFFFFF), fontSize = 13.sp)
    }
}

@Composable
fun Coin(diameter: Dp, modifier: Modifier = Modifier, spin: Float = 1f) {
    Box(
        modifier.size(diameter).graphicsLayer { scaleX = spin },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            drawCircle(
                brush = Brush.linearGradient(
                    listOf(Color(0xFFFFF59D), Color(0xFFFFB300), Color(0xFFE65100))
                ),
                radius = r
            )
            drawCircle(
                color = Color(0xFFFFE082),
                radius = r * 0.78f,
                style = Stroke(width = r * 0.1f)
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(Color(0xFFFFE082), Color(0xFFFFA000))),
                radius = r * 0.68f
            )
        }
        Text(
            "★",
            color = Color(0xFFBF360C),
            fontSize = (diameter.value * 0.45f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HandView(title: String, move: Move?, phase: Phase, mirror: Boolean, highlight: Int) {
    val inf = rememberInfiniteTransition(label = "hand")
    val bob by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(260, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val breathe by inf.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "breathe"
    )
    val pop = remember { Animatable(1f) }
    LaunchedEffect(phase) {
        if (phase == Phase.REVEALED) {
            pop.snapTo(0.2f)
            pop.animateTo(1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessLow))
        } else {
            pop.snapTo(1f)
        }
    }

    val emoji = if (phase == Phase.REVEALED && move != null) move.emoji else "✊"
    val shaking = phase == Phase.SHAKING
    val winner = highlight == 1

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Color.White, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .size(128.dp)
                .background(
                    Brush.radialGradient(
                        if (winner) listOf(Color(0xFFFFE082), Color(0x33FFC107))
                        else listOf(Color(0x66FFFFFF), Color(0x1AFFFFFF))
                    ),
                    CircleShape
                )
                .border(
                    if (winner) 4.dp else 2.dp,
                    if (winner) Gold else Color(0x55FFFFFF),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                emoji,
                fontSize = 64.sp,
                modifier = Modifier
                    .graphicsLayer {
                        val s = when {
                            shaking -> 1f
                            phase == Phase.REVEALED -> pop.value
                            else -> breathe
                        }
                        scaleX = if (mirror) -s else s
                        scaleY = s
                        translationY = if (shaking) -60f * bob else 0f
                        rotationZ = if (shaking) (bob - 0.5f) * 40f * (if (mirror) -1f else 1f) else 0f
                    }
                    .alpha(if (highlight == -1) 0.55f else 1f)
            )
        }
    }
}

@Composable
fun CoinRain(trigger: Int) {
    val progress = remember { Animatable(0f) }
    val coins = remember(trigger) {
        List(24) { Triple(Random.nextFloat(), Random.nextFloat() * 0.4f, 22 + Random.nextInt(20)) }
    }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(2400, easing = LinearEasing))
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        coins.forEachIndexed { i, (x, d, sz) ->
            val t = ((progress.value - d) / (1f - d)).coerceIn(0f, 1f)
            if (t > 0f && t < 1f) {
                val spin = abs(cos((t * 6f + i) * PI.toFloat())).coerceAtLeast(0.2f)
                Coin(
                    sz.dp,
                    Modifier.offset(
                        x = maxWidth * x - (sz / 2).dp,
                        y = maxHeight * (t * 1.1f) - 40.dp
                    ),
                    spin
                )
            }
        }
    }
}
