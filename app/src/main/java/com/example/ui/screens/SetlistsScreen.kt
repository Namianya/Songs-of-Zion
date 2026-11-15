package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SetlistWithEntries
import com.example.ui.components.HymnCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.titleFontFamilyForText
import com.example.ui.viewmodel.SetlistsViewModel

@Composable
fun SetlistsScreen(
    viewModel: SetlistsViewModel,
    onOpenHymn: (Long, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedSegment by rememberSaveable { mutableIntStateOf(0) } // 0 = Setlists, 1 = Favorites
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Worship & Favorites",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "${state.setlists.size} setlists • ${state.favorites.size} bookmarked",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedSegment == 0) {
                    FilledTonalButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("create_setlist_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Setlist")
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                SegmentedButton(
                    selected = selectedSegment == 0,
                    onClick = { selectedSegment = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_setlists_segment")
                ) {
                    Text("Setlists (${state.setlists.size})")
                }
                SegmentedButton(
                    selected = selectedSegment == 1,
                    onClick = { selectedSegment = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_favorites_segment")
                ) {
                    Text("Favorites (${state.favorites.size})")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedSegment == 0) {
                if (state.setlists.isEmpty()) {
                    EmptySetlistsState(
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        title = "No Worship Setlists Yet",
                        subtitle = "Create a setlist for Sabbath service, choir rehearsal, or Tamil fellowship to step smoothly through hymns.",
                        actionLabel = "Create First Setlist",
                        onAction = { showCreateDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.setlists, key = { it.setlist.id }) { setlistWithEntries ->
                            SetlistCard(
                                setlistWithEntries = setlistWithEntries,
                                preferredLanguageCode = state.preferredLanguageCode,
                                onOpenHymn = { hymnId ->
                                    onOpenHymn(hymnId, setlistWithEntries.setlist.id)
                                },
                                onMoveUp = { entryId ->
                                    viewModel.moveEntryUp(setlistWithEntries.setlist.id, entryId)
                                },
                                onMoveDown = { entryId ->
                                    viewModel.moveEntryDown(setlistWithEntries.setlist.id, entryId)
                                },
                                onRemoveEntry = viewModel::removeEntry,
                                onDeleteSetlist = { viewModel.deleteSetlist(setlistWithEntries.setlist.id) }
                            )
                        }
                    }
                }
            } else {
                if (state.favorites.isEmpty()) {
                    EmptySetlistsState(
                        icon = Icons.Filled.Bookmark,
                        title = "No Bookmarked Hymns Yet",
                        subtitle = "Tap the bookmark icon on any English or Tamil hymn to keep it ready for quick devotion.",
                        actionLabel = null,
                        onAction = {}
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.favorites, key = { it.hymn.id }) { item ->
                            HymnCard(
                                item = item,
                                onClick = { onOpenHymn(item.hymn.id, null) },
                                onToggleFavorite = { viewModel.removeFavorite(item.hymn.id) }
                            )
                        }
                    }
                }
            }
        }

        if (selectedSegment == 0 && state.setlists.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("fab_create_setlist"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Create setlist")
            }
        }
    }

    if (showCreateDialog) {
        CreateSetlistDialog(
            onCreate = { name, desc ->
                viewModel.createSetlist(name, desc)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun SetlistCard(
    setlistWithEntries: SetlistWithEntries,
    preferredLanguageCode: String,
    onOpenHymn: (Long) -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onDeleteSetlist: () -> Unit
) {
    val sortedEntries = remember(setlistWithEntries.entries) {
        setlistWithEntries.entries.sortedBy { it.entry.position }
    }
    var confirmDelete by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("setlist_card_${setlistWithEntries.setlist.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = setlistWithEntries.setlist.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (setlistWithEntries.setlist.description.isNotBlank()) {
                        Text(
                            text = setlistWithEntries.setlist.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${sortedEntries.size} hymns",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (sortedEntries.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { onOpenHymn(sortedEntries.first().entry.hymnId) },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("start_setlist_${setlistWithEntries.setlist.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Worship")
                        }
                    }
                    IconButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_setlist_${setlistWithEntries.setlist.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete setlist",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (sortedEntries.isEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Open any hymn and tap the playlist icon to add it here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sortedEntries.forEachIndexed { index, entryWithHymn ->
                        val h = entryWithHymn.hymnWithTranslations
                        val trans = h.translations.firstOrNull {
                            it.languageCode.equals(preferredLanguageCode, ignoreCase = true)
                        } ?: h.translations.firstOrNull()

                        if (trans != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenHymn(h.hymn.id) }
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${h.hymn.hymnNumber}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = trans.title,
                                    fontFamily = titleFontFamilyForText(trans.title, trans.languageCode),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onMoveUp(entryWithHymn.entry.id) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowUpward,
                                        contentDescription = "Move up",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onMoveDown(entryWithHymn.entry.id) },
                                    enabled = index < sortedEntries.lastIndex,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDownward,
                                        contentDescription = "Move down",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveEntry(entryWithHymn.entry.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove from setlist",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete Setlist?") },
            text = { Text("Remove “${setlistWithEntries.setlist.name}”? Your hymns remain safe in the catalog.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDeleteSetlist()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptySetlistsState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun CreateSetlistDialog(
    onCreate: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Worship Setlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Setlist Name") },
                    placeholder = { Text("e.g., Sabbath Morning Service") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_setlist_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Choir opening & communion hymns") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, description) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("dialog_create_setlist_confirm")
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
