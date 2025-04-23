package io.github.soundremote.ui.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.soundremote.BuildConfig
import io.github.soundremote.R
import io.github.soundremote.ui.components.ListItemHeadline
import io.github.soundremote.ui.components.NavigateUpButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLicense by rememberSaveable { mutableStateOf(false) }
    var licenseText by rememberSaveable { mutableStateOf("") }
    var licenseFile by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    val opusFile = "opus_license.txt"
    val apache2File = "apache_2.txt"

    LaunchedEffect(licenseFile) {
        if (licenseFile.isBlank()) return@LaunchedEffect
        licenseText = getLicense(context, licenseFile)
    }

    Column(modifier) {
        TopAppBar(
            title = {
                Text(stringResource(R.string.about_title_template).format(appName))
            },
            navigationIcon = { NavigateUpButton(onNavigateUp) },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = paddingMod
        ) {
            Text(
                text = appName + ' ' + BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = { openUrl("https://soundremote.github.io", context) },
            ) {
                Text(stringResource(R.string.open_homepage))
            }
        }
        Text(
            text = "© 2024 Aleksandr Shipovskii",
            style = MaterialTheme.typography.bodyLarge,
            modifier = paddingMod
        )
        val loadLicense: (String) -> Unit = { fileName ->
            if (fileName != licenseFile) {
                licenseText = ""
                licenseFile = fileName
            }
            showLicense = true
        }
        Credit(
            name = "Accompanist",
            onShowLicense = { loadLicense(apache2File) },
            onOpenHomepage = { openUrl("https://google.github.io/accompanist", context) }
        )
        Credit(
            name = "Guava",
            onShowLicense = { loadLicense(apache2File) },
            onOpenHomepage = { openUrl("https://guava.dev", context) }
        )
        Credit(
            name = "Hilt",
            onShowLicense = { loadLicense(apache2File) },
            onOpenHomepage = { openUrl("https://dagger.dev/hilt/", context) }
        )
        Credit(
            name = "Opus",
            onShowLicense = { loadLicense(opusFile) },
            onOpenHomepage = { openUrl("https://opus-codec.org", context) }
        )
        Credit(
            name = "Seismic",
            onShowLicense = { loadLicense(apache2File) },
            onOpenHomepage = { openUrl("https://github.com/square/seismic", context) }
        )
        Credit(
            name = "Timber",
            onShowLicense = { loadLicense(apache2File) },
            onOpenHomepage = { openUrl("https://github.com/JakeWharton/timber", context) }
        )
    }
    if (showLicense) {
        AlertDialog(
            onDismissRequest = { showLicense = false },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showLicense = false }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            text = {
                Text(
                    text = licenseText,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        )
    }
}

private fun openUrl(url: String, context: Context) {
    val webpage: Uri = Uri.parse(url)
    val intent = Intent(Intent.ACTION_VIEW, webpage)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}

private suspend fun getLicense(context: Context, fileName: String): String =
    withContext(Dispatchers.IO) {
        try {
            return@withContext context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            throw IllegalArgumentException("Failed to open asset file: $fileName", e)
        }
    }

private val paddingMod = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)

@Composable
private fun Credit(
    name: String,
    onShowLicense: () -> Unit,
    onOpenHomepage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.then(paddingMod), verticalAlignment = Alignment.CenterVertically) {
        ListItemHeadline(name, Modifier.weight(1f))
        TextButton(
            onClick = onShowLicense,
        ) {
            Text(stringResource(R.string.show_license))
        }
        TextButton(
            onClick = onOpenHomepage,
        ) {
            Text(stringResource(R.string.open_homepage))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    AboutScreen(
        onNavigateUp = {},
    )
}
