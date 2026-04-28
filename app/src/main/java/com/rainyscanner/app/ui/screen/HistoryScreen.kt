package com.rainyscanner.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.rainyscanner.app.data.ScanHistory
import com.rainyscanner.app.data.ScanRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    scanHistory: ScanHistory,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var records by remember { mutableStateOf(scanHistory.getAll()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<ScanRecord?>(null) }

    // 处理系统返回键喵~
    BackHandler(onBack = onNavigateBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "📋 扫码历史",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            actions = {
                if (records.isNotEmpty()) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = "清空历史",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (records.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🐱",
                        fontSize = MaterialTheme.typography.displayLarge.fontSize
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "还没有扫描记录喵~",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "去扫个码试试吧！",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    HistoryItem(
                        record = record,
                        onClick = { selectedRecord = record },
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("history_item", record.rawContent))
                            Toast.makeText(context, "已复制喵~ ✨", Toast.LENGTH_SHORT).show()
                        },
                        onOpen = {
                            if (record.isUrl) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(record.rawContent))
                                context.startActivity(intent)
                            }
                        },
                        onDelete = {
                            scanHistory.remove(record.id)
                            records = scanHistory.getAll()
                            Toast.makeText(context, "已删除喵~", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Bottom spacer
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "共 ${records.size} 条记录喵~ 🐾",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    // Clear all dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("清空所有历史喵？") },
            text = { Text("删除后将无法恢复哦，确定要清空吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scanHistory.clear()
                        records = scanHistory.getAll()
                        showDeleteDialog = false
                        Toast.makeText(context, "已清空喵~", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Record detail dialog
    selectedRecord?.let { record ->
        RecordDetailDialog(
            record = record,
            onDismiss = { selectedRecord = null },
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("history_detail", record.rawContent))
                Toast.makeText(context, "已复制喵~ ✨", Toast.LENGTH_SHORT).show()
            },
            onOpen = {
                if (record.isUrl) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(record.rawContent)))
                }
            },
            onDelete = {
                scanHistory.remove(record.id)
                records = scanHistory.getAll()
                selectedRecord = null
                Toast.makeText(context, "已删除喵~", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun HistoryItem(
    record: ScanRecord,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = record.displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = record.type,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = record.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Actions
            Row {
                IconButton(onClick = onCopy) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "复制",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (record.isUrl) {
                    IconButton(onClick = onOpen) {
                        Icon(
                            Icons.Default.OpenInBrowser,
                            contentDescription = "打开",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RecordDetailDialog(
    record: ScanRecord,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = record.type,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "扫描详情",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = record.rawContent,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "扫描时间: ${record.timestamp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )) {
                    Icon(Icons.Default.Delete, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除")
                }
                OutlinedButton(onClick = onCopy,
                    shape = RoundedCornerShape(20.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("复制")
                }
                if (record.isUrl) {
                    Button(onClick = onOpen,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary)) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("打开")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}