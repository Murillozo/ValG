package com.orcafacil.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val Light = lightColorScheme()
private val Dark = darkColorScheme()

@Composable
fun OrcaFacilTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Light, typography = MaterialTheme.typography, content = content)
}
