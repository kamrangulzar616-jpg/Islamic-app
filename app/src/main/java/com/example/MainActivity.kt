package com.example

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                TasbihClickerScreen()
            }
        }
    }
}

data class ZikrPhrase(
    val arabic: String,
    val englishTransliteration: String,
    val englishTranslation: String
)

val zikrPhrases = listOf(
    ZikrPhrase("سُبْحَانَ اللهِ", "Subhan Allah", "Glory be to Allah"),
    ZikrPhrase("الْحَمْدُ للهِ", "Alhamdulillah", "Praise be to Allah"),
    ZikrPhrase("اللهُ أَكْبَرُ", "Allahu Akbar", "Allah is the Greatest"),
    ZikrPhrase("لَا إِلٰهَ إِلَّا اللهُ", "La ilaha illallah", "There is no deity but Allah"),
    ZikrPhrase("أَسْتَغْفِرُ اللهَ", "Astaghfirullah", "I seek forgiveness from Allah"),
    ZikrPhrase("اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ", "Durood Shareef", "Blessings upon Prophet Muhammad")
)

@Composable
fun TasbihClickerScreen() {
    val context = LocalContext.current
    
    // Offline Data Storage
    val sharedPreferences = remember { 
        context.getSharedPreferences("tasbih_prefs", Context.MODE_PRIVATE) 
    }
    
    // State Management
    var count by remember { mutableStateOf(sharedPreferences.getInt("tasbih_count", 0)) }
    var totalLaps by remember { mutableStateOf(sharedPreferences.getInt("tasbih_laps", 0)) }
    var soundEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("sound_on", true)) }
    var vibrationEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("vibe_on", true)) }
    var selectedTarget by remember { mutableStateOf(sharedPreferences.getInt("selected_target", 33)) }
    var selectedZikrIndex by remember { mutableStateOf(sharedPreferences.getInt("selected_zikr_idx", 0)) }
    
    // Dialog States
    var showResetDialog by remember { mutableStateOf(false) }

    // Audio & Haptics Setup
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 65)
        } catch (e: Exception) {
            null
        }
    }
    val vibrator = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    // Animation scale for the central button tap feedback
    var buttonScale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = buttonScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = { buttonScale = 1f },
        label = "button_press_scale"
    )

    // Interactive Action Loop
    fun handleTap() {
        // Trigger visual tap animation
        buttonScale = 0.94f

        val newCount = count + 1
        
        // Check if target lap limit is met (33, 99, 100)
        if (newCount >= selectedTarget) {
            count = 0
            totalLaps += 1
            sharedPreferences.edit().putInt("tasbih_laps", totalLaps).apply()
            
            // Custom Long Target Completed Vibration Pattern
            if (vibrationEnabled && vibrator != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(400)
                    }
                } catch (e: Exception) {
                    // Fail-safe
                }
            }
        } else {
            count = newCount
            
            // Normal Quick Tap Feedback
            if (vibrationEnabled && vibrator != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(35)
                    }
                } catch (e: Exception) {
                    // Fail-safe
                }
            }
        }

        sharedPreferences.edit().putInt("tasbih_count", count).apply()
        
        if (soundEnabled && toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
            } catch (e: Exception) {
                // Fail-safe
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = Color(0xFF0A231C) // Spiritual Deep Emerald Green Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP BAR: Stats & Toggles ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Total Laps (Left Side - styled like Elegant Dark header)
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "TOTAL LAPS",
                            color = Color(0xFFE0A96D),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.alpha(0.8f)
                        )
                        Text(
                            text = "$totalLaps",
                            color = Color(0xFFE0A96D),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    
                    // Audio and Vibration Toggles (Right Side - rounded-full bg-[#0F3A2E] border border-white/10)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                soundEnabled = !soundEnabled
                                sharedPreferences.edit().putBoolean("sound_on", soundEnabled).apply()
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color(0xFF0F3A2E)
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Text(
                                text = if (soundEnabled) "🔊" else "🔇",
                                fontSize = 16.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                vibrationEnabled = !vibrationEnabled
                                sharedPreferences.edit().putBoolean("vibe_on", vibrationEnabled).apply()
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color(0xFF0F3A2E)
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Text(
                                text = if (vibrationEnabled) "📳" else "📴",
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Zikr Phrase Selector List
                Text(
                    text = "SELECT RECITATION",
                    color = Color(0xFF8FA39E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 6.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    itemsIndexed(zikrPhrases) { index, phrase ->
                        val isSelected = selectedZikrIndex == index
                        val borderAlpha by animateFloatAsState(if (isSelected) 1f else 0.2f, label = "border_anim")
                        val containerColor by animateColorAsState(
                            if (isSelected) Color(0xFF1E5645) else Color(0xFF0F3A2E),
                            label = "color_anim"
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(containerColor)
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFE0A96D).copy(alpha = borderAlpha),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedZikrIndex = index
                                    sharedPreferences.edit().putInt("selected_zikr_idx", index).apply()
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = phrase.arabic,
                                    color = if (isSelected) Color(0xFFE0A96D) else Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = phrase.englishTransliteration,
                                    color = if (isSelected) Color.White else Color(0xFF8FA39E),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Target Limit Selector Row (33, 99, 100 limit chips formatted beautifully as in Elegant Dark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(33, 99, 100).forEach { limit ->
                        val isSelected = selectedTarget == limit
                        val containerColor by animateColorAsState(
                            if (isSelected) Color(0xFF1E5645) else Color(0xFF0F3A2E),
                            label = "target_color"
                        )
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(containerColor)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFE0A96D) else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .alpha(if (isSelected) 1f else 0.6f)
                                .clickable {
                                    selectedTarget = limit
                                    sharedPreferences.edit().putInt("selected_target", limit).apply()
                                    count = 0 // Auto reset count safely when target switches
                                    sharedPreferences.edit().putInt("tasbih_count", 0).apply()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$limit Limit",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // --- CENTER BLOCK: Elegant Tasbih Display Core ---
            val currentPhrase = zikrPhrases.getOrElse(selectedZikrIndex) { zikrPhrases[0] }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp)
                    .scale(animatedScale)
                    .shadow(elevation = 20.dp, shape = RoundedCornerShape(40.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color(0xFF0F3A2E))
                    .border(2.dp, Color(0xFFE0A96D).copy(alpha = 0.3f), shape = RoundedCornerShape(40.dp))
                    .clickable { handleTap() },
                contentAlignment = Alignment.Center
            ) {
                // Background subtle gold decorative halo
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0A96D).copy(alpha = 0.02f))
                        .border(1.dp, Color(0xFFE0A96D).copy(alpha = 0.06f), CircleShape)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Elegant Arabic Script (using a serif look-alike)
                    Text(
                        text = currentPhrase.arabic,
                        color = Color(0xFFE0A96D),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        textAlign = TextAlign.Center,
                        lineHeight = 46.sp
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    // English Transliteration
                    Text(
                        text = currentPhrase.englishTransliteration,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    // English Meaning
                    Text(
                        text = "“${currentPhrase.englishTranslation}”",
                        color = Color(0xFF8FA39E),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Large Counter Digital readout
                    Text(
                        text = count.toString(),
                        color = Color.White,
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Target indicator label + Linear Progress Bar from Elegant Dark
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "TARGET LAP: $selectedTarget",
                            color = Color(0xFF8FA39E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Linear progress indicator
                        val progress = if (selectedTarget > 0) count.toFloat() / selectedTarget.toFloat() else 0f
                        Box(
                            modifier = Modifier
                                .width(128.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                                    .background(Color(0xFFE0A96D))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "TAP ANYWHERE TO CLICK",
                        color = Color(0xFF8FA39E).copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // --- BOTTOM BAR: Control Elements ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Reset Button
                Button(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B2635)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset All Counters",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "RESET COUNTER", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DEDICATED FOR ZIKR & DUROOD • OFFLINE ONLY",
                    color = Color(0xFF8FA39E),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.alpha(0.5f)
                )
            }
        }
    }

    // Confirmation dialog before clearing statistics
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset All Progress?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to completely reset your total laps and current count? This action cannot be undone.",
                    color = Color(0xFFC5D2D0),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        count = 0
                        totalLaps = 0
                        sharedPreferences.edit().putInt("tasbih_count", 0).apply()
                        sharedPreferences.edit().putInt("tasbih_laps", 0).apply()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B2635))
                ) {
                    Text("Yes, Reset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false }
                ) {
                    Text("Cancel", color = Color(0xFFE0A96D))
                }
            },
            containerColor = Color(0xFF0F3A2E),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, Color(0xFFE0A96D), RoundedCornerShape(16.dp))
        )
    }
}
