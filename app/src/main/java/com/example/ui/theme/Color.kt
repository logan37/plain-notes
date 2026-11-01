package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val KeepYellowLight = Color(0xFFFFF9C4)
val KeepOrangeLight = Color(0xFFFFE0B2)
val KeepMintLight = Color(0xFFC8E6C9)
val KeepTealLight = Color(0xFFB2DFDB)
val KeepSkyLight = Color(0xFFBBDEFB)
val KeepLavenderLight = Color(0xFFE1BEE7)
val KeepRoseLight = Color(0xFFFFCDD2)
val KeepSlateLight = Color(0xFFECEFF1)
val KeepWhiteLight = Color(0xFFFFFFFF)

// Dark variants
val KeepYellowDark = Color(0xFF423B1E)
val KeepOrangeDark = Color(0xFF4A301E)
val KeepMintDark = Color(0xFF1E3A24)
val KeepTealDark = Color(0xFF1C3A36)
val KeepSkyDark = Color(0xFF1E3048)
val KeepLavenderDark = Color(0xFF382346)
val KeepRoseDark = Color(0xFF452226)
val KeepSlateDark = Color(0xFF263238)
val KeepDarkSurface = Color(0xFF1E222A)

// Brand Primary & Accent
val BrandPrimary = Color(0xFFF59E0B) // Warm Amber Gold
val BrandPrimaryDark = Color(0xFFFBBF24)
val BrandSecondary = Color(0xFF3B82F6) // Secure Blue
val BrandVaultPurple = Color(0xFF8B5CF6) // Cryptographic purple

val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF1E293B)
val OutlineLight = Color(0xFFE2E8F0)

val BackgroundDark = Color(0xFF0F172A)
val SurfaceDark = Color(0xFF1E293B)
val OnSurfaceDark = Color(0xFFF1F5F9)
val OutlineDark = Color(0xFF334155)

data class NoteColorOption(
    val hex: String,
    val name: String,
    val lightColor: Color,
    val darkColor: Color
)

val NoteColorPalettes = listOf(
    NoteColorOption("#FFFFFF", "Default", KeepWhiteLight, KeepDarkSurface),
    NoteColorOption("#FFF9C4", "Lemon", KeepYellowLight, KeepYellowDark),
    NoteColorOption("#FFE0B2", "Peach", KeepOrangeLight, KeepOrangeDark),
    NoteColorOption("#C8E6C9", "Mint", KeepMintLight, KeepMintDark),
    NoteColorOption("#B2DFDB", "Sage", KeepTealLight, KeepTealDark),
    NoteColorOption("#BBDEFB", "Sky", KeepSkyLight, KeepSkyDark),
    NoteColorOption("#E1BEE7", "Lavender", KeepLavenderLight, KeepLavenderDark),
    NoteColorOption("#FFCDD2", "Coral", KeepRoseLight, KeepRoseDark),
    NoteColorOption("#ECEFF1", "Slate", KeepSlateLight, KeepSlateDark)
)
