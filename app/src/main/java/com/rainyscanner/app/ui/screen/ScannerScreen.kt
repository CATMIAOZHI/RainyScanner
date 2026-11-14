package com.rainyscanner.app.ui.screen

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.Fingerprint
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
import androidx.compose.ui.platform.LocalDensity
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
import com.rainyscanner.app.data.isFidoLink
import com.rainyscanner.app.util.SystemOpener
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.camera.core.CameraControl
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageProxy
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * 识别扫码内容的类型；二维码偶尔带的前导空白不影响判定喵~
 */
fun detectBarcodeType(rawValue: String): String {
    val value = rawValue.trimStart()
    return when {
        isFidoLink(value) -> "FIDO"
        value.startsWith("http://") || value.startsWith("https://") -> "URL"
        value.startsWith("WIFI:") -> "WIFI"
        value.startsWith("BEGIN:VCARD") || value.startsWith("MECARD:") -> "CONTACT"
        value.startsWith("MATMSG:") -> "EMAIL"
        value.startsWith("tel:") -> "PHONE"
        value.startsWith("smsto:") -> "SMS"
        value.startsWith("geo:") -> "GEO"
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

        // 框的位置按预览的真实像素算，横竖屏切换时自动重算喵
        val scanBoxRef = remember { AtomicReference<RectF?>(null) }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val containerWidthPx = constraints.maxWidth.toFloat()
            val containerHeightPx = constraints.maxHeight.toFloat()
            val scanSidePx =
                if (containerWidthPx > 0f && containerHeightPx > 0f) {
                    minOf(containerWidthPx, containerHeightPx) * SCAN_BOX_RATIO
                } else 0f

            // 把框换算成「占预览宽高的百分比」，分析帧按同样比例取 ROI，两边天然对齐喵
            SideEffect {
                scanBoxRef.set(
                    if (scanSidePx > 0f) RectF(
                        (containerWidthPx - scanSidePx) / 2f / containerWidthPx,
                        (containerHeightPx - scanSidePx) / 2f / containerHeightPx,
                        (containerWidthPx + scanSidePx) / 2f / containerWidthPx,
                        (containerHeightPx + scanSidePx) / 2f / containerHeightPx
                    ) else null
                )
            }

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
                    scanBoxRef = scanBoxRef,
                    onBarcodeDetected = { onBarcodeDetected(it) }
                )
                ScanOverlay(
                    modifier = Modifier.align(Alignment.Center),
                    scanSidePx = scanSidePx
                )
            } else {
                scannedResult?.let { result ->
                    ResultCard(
                        record = result,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("scan_result", result.rawContent))
                            Toast.makeText(context, "已复制喵~ ✨", Toast.LENGTH_SHORT).show()
                        },
                        onOpen = {
                            SystemOpener.open(
                                context = context,
                                raw = result.rawContent,
                                failureHint = if (result.isFido) SystemOpener.FIDO_FAILURE_HINT
                                               else SystemOpener.GENERIC_FAILURE_HINT
                            )
                        },
                        onScanAgain = { scannedResult = null; isScanning = true },
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    )
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun ZxingCameraPreview(
    modifier: Modifier,
    scanBoxRef: AtomicReference<RectF?>,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    // 持有 cameraProvider 引用，以便在离开页面时 unbind 相机喵！
    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // 支持的码制固定，读一遍就够了喵
    val reader = remember {
        MultiFormatReader().apply {
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
    }

    // 分辨率交给 ResolutionSelector 说清楚（16:9、1280x720），ViewPort 只管视野裁剪，两边不打架喵
    val resolutionSelector = remember {
        ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(
                    android.util.Size(1280, 720),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()
    }

    // 预览 View 的销毁标记：晚到的布局/旋转回调看到它就不再绑相机喵
    val previewDestroyed = remember { AtomicBoolean(false) }
    // 每次重绑和销毁都换一代；分析器入口拿着旧代号就把帧丢掉，
    // 结果提交时还要在主线程再核一次代号（见 bindScanUseCases 的闸门）喵
    val bindingGeneration = remember { AtomicInteger(0) }
    // 当前活着的预览 View 和它的尺寸/旋转监听器，销毁时要一个个摘掉喵
    val previewViewRef = remember { AtomicReference<PreviewView?>(null) }
    val rebindGuardRef = remember { AtomicReference<PreviewRebindGuard?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            // 先让这一代立刻作废：已经过了入口检查、还在解码的那帧，
            // 之后投递回主线程提交时会被闸门拦下，写不进旧页面喵
            bindingGeneration.incrementAndGet()
            // 再立旗子再摘监听：已经在路上的布局/旋转回调进不了绑定喵
            previewDestroyed.set(true)
            val guard = rebindGuardRef.getAndSet(null)
            // 排队的补试一起撤掉，别在销毁后再去绑相机喵
            guard?.cancel()
            previewViewRef.getAndSet(null)?.let { view ->
                guard?.let { view.removeOnLayoutChangeListener(it) }
            }
            guard?.let { watcher ->
                runCatching {
                    (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
                        ?.unregisterDisplayListener(watcher)
                }
            }
            // 先释放相机绑定，再关闭线程池喵！
            cameraProviderRef.value?.unbindAll()
            cameraExecutor.shutdownNow()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                // 预览撑满预览区，ViewPort 拿到的就是屏幕上真正看得见的视野喵
                previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                previewViewRef.set(previewView)

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    if (previewDestroyed.get()) return@addListener
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProviderRef.value = cameraProvider

                        // 真正的持续监听：转屏、分屏、折叠后尺寸或旋转变了都会重绑
                        // （原来只在 provider 就绪时 doOnLayout 跑一次，之后尺寸再变就跟不上了）喵
                        val guard = PreviewRebindGuard(
                            previewView = previewView,
                            destroyed = previewDestroyed,
                            bindAction = {
                                try {
                                    bindScanUseCases(
                                        cameraProvider = cameraProvider,
                                        lifecycleOwner = lifecycleOwner,
                                        previewView = previewView,
                                        cameraExecutor = cameraExecutor,
                                        reader = reader,
                                        resolutionSelector = resolutionSelector,
                                        scanBoxRef = scanBoxRef,
                                        previewDestroyed = previewDestroyed,
                                        onBarcodeDetected = onBarcodeDetected,
                                        mainExecutor = mainExecutor,
                                        bindingGeneration = bindingGeneration
                                    )
                                    true
                                } catch (e: Exception) {
                                    Log.e(TAG, "相机绑定失败喵", e)
                                    false
                                }
                            }
                        )
                        rebindGuardRef.set(guard)
                        previewView.addOnLayoutChangeListener(guard)
                        (ctx.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
                            ?.registerDisplayListener(guard, Handler(Looper.getMainLooper()))
                        // 尺寸已经量好就先绑一次；还没量好就等布局回调来喵
                        guard.bindNow()
                    } catch (e: Exception) {
                        Log.e(TAG, "拿不到相机喵", e)
                    }
                }, mainExecutor)
            }
        }
    )
}

/**
 * 把预览和分析绑到同一个 ViewPort：屏幕上看得见的那块，就是分析帧的解码区域喵。
 */
private fun bindScanUseCases(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    cameraExecutor: Executor,
    reader: MultiFormatReader,
    resolutionSelector: ResolutionSelector,
    scanBoxRef: AtomicReference<RectF?>,
    previewDestroyed: AtomicBoolean,
    onBarcodeDetected: (String) -> Unit,
    mainExecutor: Executor,
    bindingGeneration: AtomicInteger
) {
    // 重绑一开始就让上一代作废：哪怕某帧刚过了入口检查、还在解码，
    // 它提交结果时也会被主线程闸门拦下喵
    val generation = bindingGeneration.incrementAndGet()
    // 再解绑，旧的分析器不再有新帧进来（ZXing 不响应中断，所以不能靠 shutdown 收拾在跑的帧）
    cameraProvider.unbindAll()

    // 这一代自己的框百分比：绑定这一刻取一次快照，分析器只认这一份，
    // 不再每帧去读可能被新布局改掉的全局框，免得旧 cropRect 配上新框喵
    // （只拍了快照，没记布局代号；快照靠「尺寸/旋转变了就重绑」跟着更新，
    //  代码里没有对框和布局做配对校验）
    val boundBoxFraction = scanBoxRef.get()

    // 识别结果一律投递回主线程提交：在提交这同一个主线程任务里，重新校验
    // 「没销毁」和「当前代号还是拿到这帧的那一代」，两个都过才交给页面写状态和历史，
    // 任一不过就丢掉。后台只查一次是不够的——查完到提交之间照样可能被销毁喵
    val submitResult: (String) -> Unit = { text ->
        mainExecutor.execute {
            if (previewDestroyed.get() || bindingGeneration.get() != generation) {
                return@execute
            }
            // 页面回调现在跑在主线程，分析器那层 catch 罩不到它了；
            // 这里兜住，别让一次写状态/写历史出错把 App 崩掉（以前也是吞掉的）喵
            runCatching { onBarcodeDetected(text) }
                .onFailure { Log.w(TAG, "提交扫码结果失败喵", it) }
        }
    }

    val preview = Preview.Builder().build().also {
        it.surfaceProvider = previewView.surfaceProvider
    }

    val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setResolutionSelector(resolutionSelector)
        .build()
        .also { analysis ->
            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                try {
                    // 代号对不上说明尺寸/旋转变了已经重绑，这帧属于上一代，丢掉喵
                    // （别用新框百分比去配旧 cropRect，不然框和 ROI 就错位了）
                    if (bindingGeneration.get() != generation) return@setAnalyzer
                    // 按屏幕框住的百分比取亮度，框里对准的就是真正解码的那块喵
                    // 用的是这一代绑定时拍下的框快照，不是随时会变的全局框
                    val frame = imageProxy.scanBoxLuminance(boundBoxFraction)
                    if (frame != null) {
                        val source = com.google.zxing.PlanarYUVLuminanceSource(
                            frame.data,
                            frame.width,
                            frame.height,
                            0, 0,
                            frame.width,
                            frame.height,
                            false
                        )
                        try {
                            val result = reader.decode(
                                BinaryBitmap(HybridBinarizer(source))
                            )
                            result?.text?.let { text -> submitResult(text) }
                        } catch (e: NotFoundException) {
                            // 这一帧没读出码，很正常喵
                        }
                    }
                } catch (e: Exception) {
                    // 单帧出错不影响下一帧
                } finally {
                    imageProxy.close()
                }
            }
        }

    val useCaseGroup = UseCaseGroup.Builder()
        .addUseCase(preview)
        .addUseCase(imageAnalysis)
        .setViewPort(requireNotNull(previewView.viewPort) { "预览还没量好尺寸喵" })
        .build()

    val camera = cameraProvider.bindToLifecycle(
        lifecycleOwner,
        CameraSelector.DEFAULT_BACK_CAMERA,
        useCaseGroup
    )

    // 点哪儿对哪儿焦，凑近扫小码很管用喵
    previewView.setOnTouchListener { view, event ->
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            try {
                val point = previewView.meteringPointFactory
                    .createPoint(event.x, event.y)
                val focus = FocusMeteringAction.Builder(
                    point,
                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                ).setAutoCancelDuration(5, TimeUnit.SECONDS).build()
                val focusFuture = camera.cameraControl.startFocusAndMetering(focus)
                focusFuture.addListener({
                    try {
                        focusFuture.get()
                    } catch (e: ExecutionException) {
                        // get() 把 OperationCanceledException 包进 ExecutionException，得拆开看喵
                        val cause = e.cause
                        if (cause is CameraControl.OperationCanceledException ||
                            cause is CancellationException
                        ) {
                            // 被下一次对焦顶掉，正常取消，静默喵
                        } else {
                            Log.w(TAG, "对焦/测光请求失败喵", cause ?: e)
                        }
                    } catch (e: CancellationException) {
                        // 新的对焦请求接管了，静默喵
                    } catch (e: Exception) {
                        Log.w(TAG, "对焦/测光请求失败喵", e)
                    }
                }, mainExecutor)
            } catch (e: Exception) {
                // 个别设备不支持对焦，忽略即可
                Log.w(TAG, "这台设备不支持点按对焦喵", e)
            }
            view.performClick()
        }
        true
    }
}

/**
 * 盯着预览 View 的尺寸和屏幕旋转：只有宽高或旋转真的变了才重绑相机喵。
 *
 * 布局回调会重复来好几次，旋转回调也常常先于重新量尺寸到，
 * 所以统一用「宽 x 高 + 屏幕旋转」这一份签名去重：签名没变就一次都不重绑。
 * 绑定失败不会记成「已绑」，下一次回调或排好的补试再试；
 * 同一时刻最多留一个待执行的补试，换新签名时重置失败预算。
 */
private class PreviewRebindGuard(
    private val previewView: PreviewView,
    private val destroyed: AtomicBoolean,
    private val bindAction: () -> Boolean
) : View.OnLayoutChangeListener, DisplayManager.DisplayListener {

    /** 最近一次绑成功的签名；同一个签名不再重复绑喵 */
    private var boundSignature: Triple<Int, Int, Int>? = null
    /** 正在补试的签名，用来判断失败预算该不该重置喵 */
    private var retrySignature: Triple<Int, Int, Int>? = null
    private var retriesLeft = BIND_RETRY_MAX

    /** 补试任务本身，只留一份：排新的之前先把旧的撤掉喵 */
    private val retryRunnable = Runnable { bindNow() }

    /** 当前的「宽 x 高 + 旋转」；尺寸还没量出来就是 null 喵 */
    private fun signature(): Triple<Int, Int, Int>? {
        val width = previewView.width
        val height = previewView.height
        if (width <= 0 || height <= 0) return null
        // display.rotation 就是 Surface.ROTATION_0/90/180/270 那一套值喵
        val rotation = previewView.display?.rotation ?: 0
        return Triple(width, height, rotation)
    }

    /** provider 就绪或尺寸变化后调用；尺寸和旋转都没变就什么都不做喵 */
    fun bindNow() {
        if (destroyed.get()) return
        val signature = signature() ?: return
        if (signature == boundSignature) return
        // 换了一份新签名（首次 / 转屏 / 改尺寸）就把失败预算重置，
        // 别让上一份签名耗掉的次数拖累新布局喵
        if (signature != retrySignature) {
            retrySignature = signature
            retriesLeft = BIND_RETRY_MAX
        }
        // 动手之前先把「已绑成功」作废：bindAction 一进去就会 unbindAll，
        // 这次万一绑失败（或中途抛错），相机其实已经解绑了，
        // 不能再记着旧签名说「已经绑过」，不然从 B 退回 A 就再也绑不上了喵
        boundSignature = null
        if (bindAction()) {
            boundSignature = signature
            retrySignature = null
            retriesLeft = BIND_RETRY_MAX
            // 成功就把之前排队的补试撤掉，省得它白跑一趟喵
            removePendingRetry()
            return
        }
        // 绑失败多半是预览还没挂到窗口（viewPort 还没值）：
        // 过一会儿补试几次，别让预览一直黑着喵
        // （预算只管这几次延迟补试；布局/旋转回调每来一次仍会直接试一次）
        if (retriesLeft > 0 && !destroyed.get()) {
            retriesLeft--
            // 先撤掉可能已经在排队的那一份，保证同时只有一个待执行补试喵
            removePendingRetry()
            previewView.postDelayed(retryRunnable, BIND_RETRY_DELAY_MS)
        }
    }

    /** 销毁时把排队的补试撤掉，别再碰已经没了的预览喵 */
    fun cancel() {
        removePendingRetry()
    }

    private fun removePendingRetry() {
        previewView.removeCallbacks(retryRunnable)
    }

    override fun onLayoutChange(
        v: View,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        // 宽高没变的话签名也不会变，交给 bindNow 去重喵
        bindNow()
    }

    override fun onDisplayAdded(displayId: Int) {
        // 没有外接屏需求，忽略喵
    }

    override fun onDisplayRemoved(displayId: Int) {
        // 拔屏不影响预览，忽略喵
    }

    override fun onDisplayChanged(displayId: Int) {
        // 旋转变了也算一次真变化；若随后的布局回调带着新宽高来，会再重绑一次取新尺寸喵
        bindNow()
    }
}

@Composable
fun ScanOverlay(modifier: Modifier, scanSidePx: Float) {
    val primaryColor = MaterialTheme.colorScheme.primary
    // 框的尺寸由预览尺寸算出来，和 ROI 一个比例喵
    val scanSideDp = with(LocalDensity.current) {
        if (scanSidePx > 0f) scanSidePx.toDp() else 240.dp
    }

    // 扫描动画引擎喵！✨
    val infiniteTransition = rememberInfiniteTransition(label = "scan_wave")
    val scanLineOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 240f, // 归一化高度，下面按框的实际高度换算喵
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
        // 中间主扫描框（尺寸跟预览走，带一点科幻感的暗场遮罩）喵！
        Box(
            modifier = Modifier
                .size(scanSideDp)
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
    onOpen: () -> Unit,
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
            if (record.isFido) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "通行密钥由系统（Google Play 服务）处理，雨晴不碰你的凭据喵~",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
            }
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
                if (record.isOpenable) {
                    Button(onClick = onOpen, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary)) {
                        Icon(
                            imageVector = if (record.isFido) Icons.Default.Fingerprint
                                          else Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
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

private data class LumaFrame(val data: ByteArray, val width: Int, val height: Int)

/** 相机分析器日志标签喵 */
private const val TAG = "RainyScanner"

/** 扫描框占预览短边的比例，屏幕上的框和分析帧的 ROI 共用它喵 */
private const val SCAN_BOX_RATIO = 0.62f

/** 绑定失败后最多补试几次（预览还没挂到窗口时 viewPort 会是 null）喵 */
private const val BIND_RETRY_MAX = 5

/** 每次补试之间等多久喵 */
private const val BIND_RETRY_DELAY_MS = 200L

/** 太小的裁切解不出码，直接放弃这一帧喵 */
private const val MIN_ROI_SIDE = 64

/**
 * 把 YUV_420_888 的 Y 平面按真实行宽摊平，取出「屏幕上扫描框里」的那块正方形。
 *
 * ViewPort 会把分析帧裁成预览里看得见的视野，cropRect 就是这块；
 * 再按框在预览里的百分比换算出 ROI，框里对准的就是真正解码的区域喵。
 * 直接把 planes[0].buffer 当紧凑数组用会踩 rowStride 的对齐填充，整帧会斜着错位。
 */
private fun ImageProxy.scanBoxLuminance(boxFraction: RectF?): LumaFrame? {
    val plane = planes.firstOrNull() ?: return null
    val bufferWidth = width
    val bufferHeight = height
    if (bufferWidth <= 0 || bufferHeight <= 0) return null

    val crop = cropRect
    if (crop.width() <= 0 || crop.height() <= 0) return null
    if (crop.left < 0 || crop.top < 0 ||
        crop.right > bufferWidth || crop.bottom > bufferHeight
    ) return null

    // rotationDegrees 不为 0 时，屏幕上的竖幅对应 buffer 里横过来的那块喵
    val rotation = imageInfo.rotationDegrees
    val swapped = rotation == 90 || rotation == 270
    val uprightWidth = if (swapped) crop.height() else crop.width()
    val uprightHeight = if (swapped) crop.width() else crop.height()
    if (uprightWidth < MIN_ROI_SIDE || uprightHeight < MIN_ROI_SIDE) return null

    // 先在「竖直画面」里算出框的像素范围
    val leftU: Int
    val topU: Int
    val side: Int
    if (boxFraction != null) {
        val l = (boxFraction.left * uprightWidth).toInt()
        val t = (boxFraction.top * uprightHeight).toInt()
        val r = (boxFraction.right * uprightWidth).toInt()
        val b = (boxFraction.bottom * uprightHeight).toInt()
        val boxWidth = r - l
        val boxHeight = b - t
        if (boxWidth <= 0 || boxHeight <= 0) return null
        // 边长同时压到源帧短边以内，绝不越界
        side = minOf(boxWidth, boxHeight, uprightWidth, uprightHeight)
        if (side < MIN_ROI_SIDE) return null
        leftU = (l + boxWidth / 2 - side / 2).coerceIn(0, uprightWidth - side)
        topU = (t + boxHeight / 2 - side / 2).coerceIn(0, uprightHeight - side)
    } else {
        // 还没量到框的尺寸时，退回画面正中同比例的一块喵
        side = (minOf(uprightWidth, uprightHeight) * SCAN_BOX_RATIO).toInt()
            .coerceAtMost(minOf(uprightWidth, uprightHeight))
        if (side < MIN_ROI_SIDE) return null
        leftU = (uprightWidth - side) / 2
        topU = (uprightHeight - side) / 2
    }

    // 竖直画面的坐标 → buffer 坐标（正方形转了还是正方形，位置要跟着转）
    val localLeft: Int
    val localTop: Int
    when (rotation) {
        90 -> {
            localLeft = topU
            localTop = crop.height() - leftU - side
        }
        180 -> {
            localLeft = crop.width() - leftU - side
            localTop = crop.height() - topU - side
        }
        270 -> {
            localLeft = crop.width() - topU - side
            localTop = leftU
        }
        else -> {
            localLeft = leftU
            localTop = topU
        }
    }
    val left = crop.left + localLeft
    val top = crop.top + localTop
    if (left < 0 || top < 0 || left + side > bufferWidth || top + side > bufferHeight) return null

    val rowStride = plane.rowStride
    val pixelStride = maxOf(1, plane.pixelStride)
    if (rowStride <= 0) return null

    val buffer = plane.buffer.duplicate()
    buffer.rewind()
    val limit = buffer.limit()
    if (limit <= 0) return null

    val lastColumnOffset = (left + side - 1) * pixelStride
    val lastRowIndex = top + side - 1
    // 帧不够大就是异常帧，直接不要，别拿黑边凑数喵
    if (lastRowIndex.toLong() * rowStride + lastColumnOffset >= limit) return null

    val out = ByteArray(side * side)
    val rowBuffer = ByteArray(rowStride)

    for (y in 0 until side) {
        val rowStart = (top + y) * rowStride
        val available = minOf(rowStride, limit - rowStart)
        if (available <= lastColumnOffset) return null
        buffer.position(rowStart)
        buffer.get(rowBuffer, 0, available)
        var srcX = left * pixelStride
        var dstIndex = y * side
        for (x in 0 until side) {
            out[dstIndex] = rowBuffer[srcX]
            srcX += pixelStride
            dstIndex++
        }
    }
    return LumaFrame(out, side, side)
}
