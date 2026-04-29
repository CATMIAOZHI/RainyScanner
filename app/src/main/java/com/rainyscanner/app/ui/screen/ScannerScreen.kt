package com.rainyscanner.app.ui.screen

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.rainyscanner.app.data.ScanHistory
import com.rainyscanner.app.data.ScanRecord
import java.nio.ByteBuffer
import java.util.concurrent.Executors

fun detectBarcodeType(rawValue: String): String {
    return when {
        rawValue.startsWith("http://") || rawValue.startsWith("https://") -> "URL"
        rawValue.startsWith("WIFI:") -> "WIFI"
        rawValue.startsWith("BEGIN:VCARD") || rawValue.startsWith("MECARD:") -> "CONTACT"
        rawValue.startsWith("MATMSG:") -> "EMAIL"
        rawValue.startsWith("tel:") -> "PHONE"
        rawValue.startsWith("smsto:") -> "SMS"
        rawValue.startsWith("geo:") -> "GEO"
        else -> "TEXT"
    }
}

/**
 * 从 Bitmap 解码条码/二维码，返回原始文本或 null。
 */
fun decodeBitmap(bitmap: android.graphics.Bitmap): String? {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
    val binarizer = HybridBinarizer(source)
    val binaryBitmap = BinaryBitmap(binarizer)

    val reader = MultiFormatReader().apply {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.PDF_417,
                BarcodeFormat.AZTEC
            ),
            DecodeHintType.TRY_HARDER to true
        )
        setHints(hints)
    }

    return try {
        reader.decode(binaryBitmap).text
    } catch (e: NotFoundException) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    scanHistory: ScanHistory,
    onNavigateToHistory: () -> Unit,
    onNavigateToAbout: () -> Unit = {}
) {
    val context = LocalContext.current
    var scannedResult by remember { mutableStateOf<ScanRecord?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var hasPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    )}

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            Toast.makeText(context, "喵~ 需要相机权限才能扫码哦！", Toast.LENGTH_SHORT).show()
        }
    }

    // 相册选图解码喵~
    var isDecodingImage by remember { mutableStateOf(false) }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isDecodingImage = true
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        val result = decodeBitmap(bitmap)
                        bitmap.recycle()
                        if (result != null) {
                            val record = ScanRecord(rawContent = result, type = detectBarcodeType(result))
                            scannedResult = record
                            scanHistory.add(record)
                            isScanning = false
                        } else {
                            // 在主线程弹提示
                            (context as? android.app.Activity)?.runOnUiThread {
                                Toast.makeText(context, "喵~ 图片中没有识别到条码", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    (context as? android.app.Activity)?.runOnUiThread {
                        Toast.makeText(context, "喵~ 读取图片失败", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    isDecodingImage = false
                    executor.shutdown()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun onBarcodeDetected(rawValue: String) {
        if (scannedResult != null) return
        val type = detectBarcodeType(rawValue)
        val record = ScanRecord(rawContent = rawValue, type = type)
        scannedResult = record
        scanHistory.add(record)
        isScanning = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "雨晴扫描",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            actions = {
                IconButton(onClick = onNavigateToAbout) {
                    Icon(Icons.Default.Info, contentDescription = "关于",
                        tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    enabled = !isDecodingImage
                ) {
                    Icon(Icons.Default.Image, contentDescription = "相册扫码",
                        tint = if (isDecodingImage) MaterialTheme.colorScheme.onSurfaceVariant
                               else MaterialTheme.colorScheme.primary)
                }
                if (scannedResult != null) {
                    IconButton(onClick = {
                        scannedResult = null
                        isScanning = true
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "重新扫码",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onNavigateToHistory) {
                    Icon(Icons.Default.History, contentDescription = "历史记录",
                        tint = MaterialTheme.colorScheme.primary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Box(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (!hasPermission) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)) {
                    Text(text = "🐱", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "喵~ 需要相机权限才能扫码",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(24.dp)
                    ) { Text("授予权限") }
                }
            } else if (isScanning) {
                ZxingCameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onBarcodeDetected = { onBarcodeDetected(it) }
                )
                ScanOverlay(modifier = Modifier.align(Alignment.Center))
            } else {
                scannedResult?.let { result ->
                    ResultCard(
                        record = result,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("scan_result", result.rawContent))
                            Toast.makeText(context, "已复制喵~ ✨", Toast.LENGTH_SHORT).show()
                        },
                        onOpenBrowser = {
                            if (result.isUrl) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.rawContent)))
                            }
                        },
                        onScanAgain = { scannedResult = null; isScanning = true },
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ZxingCameraPreview(
    modifier: Modifier,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // 持有 cameraProvider 引用，以便在离开页面时 unbind 相机喵！
    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            // 先释放相机绑定，再关闭线程池喵！
            cameraProviderRef.value?.unbindAll()
            cameraExecutor.shutdownNow()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProviderRef.value = cameraProvider

                        // 安全起见先解绑，避免重复绑定冲突喵！
                        cameraProvider.unbindAll()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val reader = MultiFormatReader().apply {
                            val hints = mapOf(
                                DecodeHintType.POSSIBLE_FORMATS to listOf(
                                    BarcodeFormat.QR_CODE,
                                    BarcodeFormat.CODE_128,
                                    BarcodeFormat.CODE_39,
                                    BarcodeFormat.EAN_13,
                                    BarcodeFormat.EAN_8,
                                    BarcodeFormat.DATA_MATRIX,
                                    BarcodeFormat.PDF_417,
                                    BarcodeFormat.AZTEC
                                ),
                                DecodeHintType.TRY_HARDER to true
                            )
                            setHints(hints)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    try {
                                        val planes = imageProxy.planes
                                        if (planes.isNotEmpty()) {
                                            val buffer: ByteBuffer = planes[0].buffer
                                            val data = ByteArray(buffer.remaining())
                                            buffer.get(data)

                                            val source = com.google.zxing.PlanarYUVLuminanceSource(
                                                data,
                                                imageProxy.width,
                                                imageProxy.height,
                                                0, 0,
                                                imageProxy.width,
                                                imageProxy.height,
                                                false
                                            )
                                            val binarizer = HybridBinarizer(source)
                                            val bitmap = BinaryBitmap(binarizer)
                                            try {
                                                val result = reader.decode(bitmap)
                                                result?.text?.let { text ->
                                                    onBarcodeDetected(text)
                                                }
                                            } catch (e: NotFoundException) {
                                                // No barcode found, that's fine
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // Ignore decoding errors
                                    } finally {
                                        imageProxy.close()
                                    }
                                }
                            }

                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        }
    )
}

@Composable
fun ScanOverlay(modifier: Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // 扫描动画引擎喵！✨
    val infiniteTransition = rememberInfiniteTransition(label = "scan_wave")
    val scanLineOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 240f, // 和扫描框本身高度一致
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line_offset"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 中间主扫描框（带有科幻感的暗场遮罩）喵！
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
        ) {
            // 利用 Canvas 绘制赛博朋克扫描角和软萌激光喵！
            Canvas(modifier = Modifier.fillMaxSize()) {
                val size = size
                val cornerLength = 40f  // 拐角的长度喵
                val strokeWidth = 10f   // 可爱的饱满粗线条喵
                val cornerRadius = 24f  // 拐角处的圆滑弧度喵

                val path = Path()

                // 左上角包边喵
                path.moveTo(0f, cornerLength)
                path.lineTo(0f, cornerRadius)
                path.quadraticBezierTo(0f, 0f, cornerRadius, 0f)
                path.lineTo(cornerLength, 0f)

                // 右上角包边喵
                path.moveTo(size.width - cornerLength, 0f)
                path.lineTo(size.width - cornerRadius, 0f)
                path.quadraticBezierTo(size.width, 0f, size.width, cornerRadius)
                path.lineTo(size.width, cornerLength)

                // 左下角包边喵
                path.moveTo(0f, size.height - cornerLength)
                path.lineTo(0f, size.height - cornerRadius)
                path.quadraticBezierTo(0f, size.height, cornerRadius, size.height)
                path.lineTo(cornerLength, size.height)

                // 右下角包边喵
                path.moveTo(size.width - cornerLength, size.height)
                path.lineTo(size.width - cornerRadius, size.height)
                path.quadraticBezierTo(size.width, size.height, size.width, size.height - cornerRadius)
                path.lineTo(size.width, size.height - cornerLength)

                // 绘制粉粉的圆润猫耳边角喵！
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // 画一条纯白带发光感的动态扫描激光喵！
                val dpToPxScale = size.height / 240f // 适配偏移量喵
                val currentY = scanLineOffsetY * dpToPxScale

                // 绘制渐变光带（往返光波拖尾）喵~
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryColor.copy(alpha = 0.4f),
                            Color.White
                        ),
                        startY = currentY - 60f, // 拖尾的起始高度
                        endY = currentY
                    ),
                    topLeft = Offset(10f, currentY - 60f),
                    size = androidx.compose.ui.geometry.Size(size.width - 20f, 60f)
                )

                // 光波的最边缘亮白射线喵！
                drawLine(
                    color = Color.White,
                    start = Offset(10f, currentY),
                    end = Offset(size.width - 10f, currentY),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
        }

        // 提示文字背景块 + 文字 — 底部居中
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp) // 往上提一点点喵
        ) {
            Text(
                text = "✨对准二维码，雨晴帮你扫喵！💕",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun ResultCard(
    record: ScanRecord,
    onCopy: () -> Unit,
    onOpenBrowser: () -> Unit,
    onScanAgain: () -> Unit,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "✨", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "扫描成功喵~", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = record.type, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = record.rawContent, modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 8, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "扫描时间: ${record.timestamp}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("复制")
                }
                if (record.isUrl) {
                    Button(onClick = onOpenBrowser, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary)) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("打开")
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) {
                Text("继续扫码 🔄", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}