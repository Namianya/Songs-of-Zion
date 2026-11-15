package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NightReaderAccent
import com.example.ui.theme.NightReaderBackground
import com.example.ui.theme.NightReaderCard
import com.example.ui.theme.NightReaderInk
import com.example.ui.theme.NightReaderMuted
import com.example.ui.theme.NightReaderOutline
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.lyricTextStyle
import com.example.ui.theme.titleFontFamilyForText
import com.example.ui.viewmodel.ReaderViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HymnReaderScreen(
    viewModel: ReaderViewModel,
    onBack: () -> Unit,
    onNavigateToHymn: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isNightMode = state.preferences.readerNightMode

    val backgroundColor by animateColorAsState(
        targetValue = if (isNightMode) NightReaderBackground else MaterialTheme.colorScheme.background,
        label = "reader_bg"
    )
    val cardColor by animateColorAsState(
        targetValue = if (isNightMode) NightReaderCard else MaterialTheme.colorScheme.surfaceContainer,
        label = "reader_card"
    )
    val textColor by animateColorAsState(
        targetValue = if (isNightMode) NightReaderInk else MaterialTheme.colorScheme.onBackground,
        label = "reader_ink"
    )
    val mutedColor by animateColorAsState(
        targetValue = if (isNightMode) NightReaderMuted else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "reader_muted"
    )
    val accentColor by animateColorAsState(
        targetValue = if (isNightMode) NightReaderAccent else MaterialTheme.colorScheme.secondary,
        label = "reader_accent"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isNightMode) NightReaderOutline else MaterialTheme.colorScheme.outlineVariant,
        label = "reader_border"
    )

    var showMetadata by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showSetlistSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = backgroundColor,
        topBar = {
            Surface(
                color = backgroundColor,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }

                    val hymn = state.hymn
                    if (hymn != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isNightMode) NightReaderCard else MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Text(
                                text = "#${hymn.hymnNumber}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isNightMode) NightReaderAccent else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    state.navigationContextLabel?.let { label ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = mutedColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    } ?: Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = { viewModel.setReaderNightMode(!isNightMode) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reader_night_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NightsStay,
                            contentDescription = "Toggle Sanctuary Night Mode",
                            tint = if (isNightMode) NightReaderAccent else mutedColor
                        )
                    }

                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reader_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FormatSize,
                            contentDescription = stringResource(R.string.reader_settings),
                            tint = textColor
                        )
                    }

                    IconButton(
                        onClick = { showSetlistSheet = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reader_add_setlist_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Add to worship setlist",
                            tint = textColor
                        )
                    }

                    IconButton(
                        onClick = viewModel::toggleFavorite,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reader_favorite_button")
                    ) {
                        val fav = state.hymn?.isFavorite == true
                        Icon(
                            imageVector = if (fav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (fav) {
                                stringResource(R.string.favorite_remove)
                            } else {
                                stringResource(R.string.favorite_add)
                            },
                            tint = if (fav) accentColor else textColor
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (state.previousHymnId != null || state.nextHymnId != null) {
                Surface(
                    color = cardColor,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { state.previousHymnId?.let(onNavigateToHymn) },
                            enabled = state.previousHymnId != null,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("reader_prev_hymn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = if (state.previousHymnId != null) accentColor else mutedColor.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Previous",
                                color = if (state.previousHymnId != null) textColor else mutedColor.copy(alpha = 0.4f)
                            )
                        }

                        Text(
                            text = "${state.preferences.fontSizeSp.roundToInt()} sp",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = mutedColor
                        )

                        TextButton(
                            onClick = { state.nextHymnId?.let(onNavigateToHymn) },
                            enabled = state.nextHymnId != null,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("reader_next_hymn")
                        ) {
                            Text(
                                text = "Next",
                                color = if (state.nextHymnId != null) textColor else mutedColor.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = if (state.nextHymnId != null) accentColor else mutedColor.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val hymn = state.hymn
        val translation = state.activeTranslation

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (hymn == null || translation == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Hymn not found.", color = textColor)
            }
        } else {
            val scrollState = rememberScrollState(hymn.id.toInt())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // Category pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accentColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = hymn.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Title in active language
                    Text(
                        text = translation.title,
                        fontFamily = titleFontFamilyForText(translation.title, translation.languageCode),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 27.sp,
                        lineHeight = 36.sp,
                        color = textColor
                    )

                    // Parallel Title in other language
                    state.parallelTitle?.let { parallel ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = parallel,
                            fontFamily = titleFontFamilyForText(parallel),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = mutedColor
                        )
                    }

                    // Translation Switcher Bar (English | தமிழ் | ...)
                    val switcherLanguages = remember(state.translations) {
                        val existing = state.translations.map { it.languageCode.lowercase() to it.languageName }
                        if (existing.any { it.first == "ta" }) {
                            existing
                        } else {
                            existing + ("ta" to "தமிழ்")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        switcherLanguages.forEach { (code, name) ->
                            val isSelected = if (state.selectedLanguageCode.equals(code, ignoreCase = true)) {
                                true
                            } else if (code == translation.languageCode.lowercase() && !state.isTranslatingTamil && state.translationError == null && state.selectedLanguageCode != "ta") {
                                true
                            } else {
                                false
                            }
                            val isUncachedTamil = code == "ta" && !state.hasTamilTranslation

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(enabled = !state.isTranslatingTamil) {
                                        viewModel.selectLanguage(code)
                                    }
                                    .testTag("reader_lang_$code"),
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) {
                                    if (isNightMode) NightReaderAccent else MaterialTheme.colorScheme.primary
                                } else {
                                    cardColor
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color.Transparent else borderColor
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isUncachedTamil) {
                                        Icon(
                                            imageVector = Icons.Filled.Translate,
                                            contentDescription = null,
                                            tint = if (isSelected) {
                                                if (isNightMode) NightReaderBackground else MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                accentColor
                                            },
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) {
                                            if (isNightMode) NightReaderBackground else MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            textColor
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (state.isTranslatingTamil) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("translating_tamil_card"),
                            shape = MaterialTheme.shapes.medium,
                            color = cardColor,
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = accentColor
                                )
                                Text(
                                    text = stringResource(R.string.translating_tamil),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                                )
                            }
                        }
                    } else if (state.translationError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("translation_error_card"),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = state.translationError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = viewModel::translateCurrentHymnToTamil,
                                        modifier = Modifier.testTag("retry_tamil_translation")
                                    ) {
                                        Text(stringResource(R.string.retry_translation))
                                    }
                                    TextButton(
                                        onClick = { viewModel.selectLanguage("en") },
                                        modifier = Modifier.testTag("fallback_english_button")
                                    ) {
                                        Text(stringResource(R.string.lang_english))
                                    }
                                }
                            }
                        }
                    } else if (state.selectedLanguageCode == "ta" && !state.hasTamilTranslation) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("prompt_translate_tamil_card"),
                            shape = MaterialTheme.shapes.medium,
                            color = cardColor,
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Tamil lyrics not cached offline yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = mutedColor,
                                    modifier = Modifier.weight(1f)
                                )
                                FilledTonalButton(
                                    onClick = viewModel::translateCurrentHymnToTamil,
                                    modifier = Modifier.testTag("translate_tamil_button")
                                ) {
                                    Text(stringResource(R.string.translate_to_tamil))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Collapsible Tune & Heritage Metadata Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { showMetadata = !showMetadata }
                            .testTag("reader_metadata_toggle"),
                        shape = MaterialTheme.shapes.medium,
                        color = cardColor,
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = buildString {
                                            append(hymn.tuneName.ifBlank { "Tune & Heritage Details" })
                                            if (hymn.musicalKey.isNotBlank()) append(" • ${hymn.musicalKey}")
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = textColor
                                    )
                                }
                                Icon(
                                    imageVector = if (showMetadata) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (showMetadata) "Collapse details" else "Expand details",
                                    tint = mutedColor
                                )
                            }

                            AnimatedVisibility(visible = showMetadata) {
                                Column(
                                    modifier = Modifier.padding(top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    HorizontalDivider(color = borderColor)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    MetadataRow("Meter", hymn.meter, textColor, mutedColor)
                                    MetadataRow("Key", hymn.musicalKey, textColor, mutedColor)
                                    MetadataRow("Author", hymn.author, textColor, mutedColor)
                                    MetadataRow("Scripture", hymn.scriptureRef, textColor, mutedColor)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Structured Stanzas & Refrains
                    val lyricStyle = remember(translation.languageCode, state.preferences.fontSizeSp) {
                        lyricTextStyle(translation.languageCode, state.preferences.fontSizeSp)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        state.sections.forEach { section ->
                            if (section.isChorus) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (isNightMode) {
                                        NightReaderCard
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        accentColor.copy(alpha = 0.35f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        Text(
                                            text = section.label.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accentColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = section.lines.joinToString("\n"),
                                            style = lyricStyle.copy(
                                                fontStyle = if (translation.languageCode == "ta") {
                                                    FontStyle.Normal
                                                } else {
                                                    FontStyle.Italic
                                                }
                                            ),
                                            color = textColor
                                        )
                                    }
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = section.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text(
                                        text = section.lines.joinToString("\n"),
                                        style = lyricStyle,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }
    }

    if (showSettingsSheet) {
        ReaderSettingsSheet(
            fontSizeSp = state.preferences.fontSizeSp,
            nightMode = isNightMode,
            themeMode = state.preferences.themeMode,
            onFontSizeChange = viewModel::setFontSize,
            onNightModeChange = viewModel::setReaderNightMode,
            onThemeModeChange = viewModel::setThemeMode,
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (showSetlistSheet) {
        AddToSetlistSheet(
            memberships = state.setlistMemberships,
            onToggleSetlist = viewModel::toggleHymnInSetlist,
            onCreateAndAdd = viewModel::createSetlistAndAddHymn,
            onDismiss = { showSetlistSheet = false }
        )
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
    textColor: Color,
    mutedColor: Color
) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = mutedColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ReaderSettingsSheet(
    fontSizeSp: Float,
    nightMode: Boolean,
    themeMode: ThemeMode,
    onFontSizeChange: (Float) -> Unit,
    onNightModeChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.reader_settings),
                style = MaterialTheme.typography.titleLarge
            )

            // Font size slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lyric Font Size",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${fontSizeSp.roundToInt()} sp",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = fontSizeSp,
                    onValueChange = onFontSizeChange,
                    valueRange = 14f..28f,
                    steps = 6,
                    modifier = Modifier.testTag("reader_font_slider")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Compact (14)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Large (28)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider()

            // Night Reading Mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sanctuary Night Mode",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Low-glare dark canvas with warm ivory lyrics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = nightMode,
                    onCheckedChange = onNightModeChange,
                    modifier = Modifier.testTag("reader_night_switch")
                )
            }

            HorizontalDivider()

            // App Theme Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "App Theme",
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = mode == themeMode
                        FilledTonalButton(
                            onClick = { onThemeModeChange(mode) },
                            modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(mode.label)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToSetlistSheet(
    memberships: List<com.example.ui.viewmodel.SetlistMembership>,
    onToggleSetlist: (Long) -> Unit,
    onCreateAndAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Add to Worship Setlist",
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("New setlist name (e.g., Sabbath Service)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_setlist_inline_input")
                )
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onCreateAndAdd(newName)
                            newName = ""
                        }
                    },
                    enabled = newName.isNotBlank(),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("create_setlist_inline_button")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Create and add")
                }
            }

            if (memberships.isEmpty()) {
                Text(
                    text = "No worship setlists yet. Create one above to queue hymns for service.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(memberships, key = { it.setlist.id }) { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleSetlist(item.setlist.id) }
                                .testTag("setlist_option_${item.setlist.id}"),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.containsHymn,
                                    onCheckedChange = { onToggleSetlist(item.setlist.id) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.setlist.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "${item.hymnCount} hymns",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
