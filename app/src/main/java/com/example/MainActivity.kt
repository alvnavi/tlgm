package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ChatRoom
import com.example.model.Contact
import com.example.model.Message
import com.example.model.Plugin
import com.example.ui.theme.*
import com.example.viewmodel.TeleViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TeleGuardApp()
            }
        }
    }
}

@Composable
fun TeleGuardApp() {
    val viewModel: TeleViewModel = viewModel()
    
    // Bottom tab routing representing modular sections
    var activeTab by remember { mutableStateOf("chats") } // "chats", "keys", "plugins"
    val activeChatRoom by viewModel.selectedChatRoom.collectAsState()
    
    // Responsive state: if in mobile vertical and we have an active room, show full screen chat message detail
    var showMobileChatDetail by remember { mutableStateOf(false) }
    
    LaunchedEffect(activeChatRoom) {
        if (activeChatRoom != null) {
            showMobileChatDetail = true
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_root"),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!showMobileChatDetail || activeChatRoom == null) {
                TeleGuardBottomNavigation(
                    activeTab = activeTab,
                    onTabSelected = { 
                        activeTab = it 
                        // Automatically close detail on navigation change
                        showMobileChatDetail = false
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "chats" -> {
                    ChatsDashboardScreen(
                        viewModel = viewModel,
                        showDetail = showMobileChatDetail,
                        onBackToList = { showMobileChatDetail = false },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                "keys" -> {
                    MyKeysStudioScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                "plugins" -> {
                    PluginsCenterScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun TeleGuardBottomNavigation(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = SlateSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = activeTab == "chats",
            onClick = { onTabSelected("chats") },
            icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
            label = { Text("Chats", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SecureGreen,
                selectedTextColor = SecureGreen,
                indicatorColor = SlateSurfaceVariant,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        NavigationBarItem(
            selected = activeTab == "keys",
            onClick = { onTabSelected("keys") },
            icon = { Icon(Icons.Default.Key, contentDescription = "Llaves GPG/EC") },
            label = { Text("Mis Llaves", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SecureGreen,
                selectedTextColor = SecureGreen,
                indicatorColor = SlateSurfaceVariant,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        NavigationBarItem(
            selected = activeTab == "plugins",
            onClick = { onTabSelected("plugins") },
            icon = { Icon(Icons.Default.Extension, contentDescription = "Modificaciones") },
            label = { Text("Plugins", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SecureGreen,
                selectedTextColor = SecureGreen,
                indicatorColor = SlateSurfaceVariant,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
    }
}

// --- CHATS MAIN ROUTE ---

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ChatsDashboardScreen(
    viewModel: TeleViewModel,
    showDetail: Boolean,
    onBackToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rooms by viewModel.chatRooms.collectAsState()
    val activeRoom by viewModel.selectedChatRoom.collectAsState()
    
    // Add contact dialog trigger
    var showAddContactDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier) {
        val isWideScreen = maxWidth > 720.dp

        if (isWideScreen) {
            // Tablet/Desktop Split view layout
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Panel: Chats List
                ChatsListPanel(
                    viewModel = viewModel,
                    rooms = rooms,
                    activeRoom = activeRoom,
                    onRoomSelected = { viewModel.selectChatRoom(it) },
                    onAddContactClick = { showAddContactDialog = true },
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .background(SlateDarkBg)
                )

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(BorderColor)
                )

                // Right Panel: Active Chat
                Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                    if (activeRoom != null) {
                        ActiveChatPanel(
                            viewModel = viewModel,
                            room = activeRoom!!,
                            onBackClick = {}, // Not needed on tablet split view
                            showBack = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Empty states feedback
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SlateSurface)
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = "Escudo",
                                modifier = Modifier.size(72.dp),
                                tint = TelegramBlue.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Selecciona una conversación",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Todo mensaje enviado a través de canales TeleGuard se encriptará punto a punto en el terminal de salida antes de transitar por Telegram.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.widthIn(max = 300.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            // Mobile: Swap screens based on selection
            AnimatedContent(
                targetState = showDetail && activeRoom != null,
                transitionSpec = {
                    if (targetState) {
                        slideInHorizontally { width -> width / 2 } + fadeIn() with
                                slideOutHorizontally { width -> -width / 2 } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width / 2 } + fadeIn() with
                                slideOutHorizontally { width -> width / 2 } + fadeOut()
                    }
                },
                label = "mobile_navigation_anim"
            ) { isDetailVisible ->
                if (isDetailVisible && activeRoom != null) {
                    ActiveChatPanel(
                        viewModel = viewModel,
                        room = activeRoom!!,
                        onBackClick = onBackToList,
                        showBack = true,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    ChatsListPanel(
                        viewModel = viewModel,
                        rooms = rooms,
                        activeRoom = activeRoom,
                        onRoomSelected = { 
                            viewModel.selectChatRoom(it)
                        },
                        onAddContactClick = { showAddContactDialog = true },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SlateDarkBg)
                    )
                }
            }
        }

        if (showAddContactDialog) {
            AddContactDialog(
                onDismiss = { showAddContactDialog = false },
                onAddContact = { name, bio, isSecure ->
                    viewModel.addNewContact(name, bio, isSecure)
                    showAddContactDialog = false
                }
            )
        }
    }
}

@Composable
fun ChatsListPanel(
    viewModel: TeleViewModel,
    rooms: List<ChatRoom>,
    activeRoom: ChatRoom?,
    onRoomSelected: (ChatRoom) -> Unit,
    onAddContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // App Identity Header (Material You custom branding)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateSurface)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TelegramBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = "Shield Logo",
                        tint = SlateDarkBg,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "TeleGuard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Capa Criptográfica Segura v1.2",
                        fontSize = 10.sp,
                        color = SecureGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            IconButton(
                onClick = onAddContactClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SlateSurfaceVariant)
                    .testTag("add_contact_button")
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = "Añadir Socio",
                    tint = TextPrimary
                )
            }
        }

        // Encryption Badge Status Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateSurfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(SecureGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Motores de encriptación ECDH, AES-256 y GPG activos",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }

        if (rooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = "No chats",
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No hay conversaciones activas",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Presiona el botón superior para agregar un socio e interactuar con cifrado asimétrico.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("chats_list"),
                contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
            ) {
                // Incorporate the Bento Dashboard directly as the first top layout item!
                item {
                    BentoDashboardHeader(
                        viewModel = viewModel,
                        activeRoom = activeRoom,
                        onRoomSelected = onRoomSelected,
                        rooms = rooms
                    )
                }

                item {
                    Text(
                        text = "CONVERSACIONES RECIENTES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        letterSpacing = 1.sp
                    )
                }

                items(rooms) { room ->
                    val isSelected = activeRoom != null && activeRoom.id == room.id
                    ChatRoomRow(
                        room = room,
                        isSelected = isSelected,
                        onClick = { onRoomSelected(room) }
                    )
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.3f), thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
fun BentoDashboardHeader(
    viewModel: TeleViewModel,
    activeRoom: ChatRoom?,
    onRoomSelected: (ChatRoom) -> Unit,
    rooms: List<ChatRoom>
) {
    val logs by viewModel.engineLogs.collectAsState()
    val pluginsList by viewModel.plugins.collectAsState()
    val activePluginsCount = pluginsList.count { it.isEnabled }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- CELDA BENTO 1: Sesión Activa (Ancho completo) ---
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(SecureGreen.copy(alpha = 0.5f), TelegramBlue.copy(alpha = 0.5f))),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "SESIÓN SECURE-OTA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = SecureGreen,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeRoom?.name ?: "TeleGuard Desconectado",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (activeRoom != null) SecureGreen.copy(alpha = 0.15f) else SlateSurfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = activeRoom?.defaultEncryptionType ?: "SIN ALGORITMO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeRoom != null) SecureGreen else TextSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Fingerprint or status visual block
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing green dot
                    val animatedDotColor = SecureGreen
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (activeRoom != null) animatedDotColor else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    val shortFingerprint = if (activeRoom != null) {
                        "EC: 8F2A...B7E1 | RSA: Active Pair"
                    } else "Esperando conexión con dispositivo par..."
                    
                    Text(
                        text = shortFingerprint,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (activeRoom != null) {
                                onRoomSelected(activeRoom)
                            } else if (rooms.isNotEmpty()) {
                                onRoomSelected(rooms.first())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecureGreen, contentColor = SlateDarkBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (activeRoom != null) "Abrir Canal Seguro" else "Conectar con Alicia",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            viewModel.rotateSovereignKeys()
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SlateSurfaceVariant)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = "Rotar Claves",
                            tint = SecureGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        
        // --- CELDA DE FILA BENTO GRID: Dos columnas ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Celda Bento 2: Switche de Cifrado Forzado
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
                    .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Safe",
                        tint = SecureGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Column {
                        Text(
                            text = "Auto-Cifrar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Siempre On",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            // Elegant static dot indicator simulating switch active
                            Box(
                                modifier = Modifier
                                    .size(16.dp, 10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(SecureGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 2.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SecureGreen)
                                )
                            }
                        }
                    }
                }
            }
            
            // Celda Bento 3: Plugins Status
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
                    .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Plugins",
                        tint = TelegramBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Column {
                        Text(
                            text = "Módulos",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "$activePluginsCount activos",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("GPG", "ECDH", "EMOJI").forEach { text ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SlateSurfaceVariant)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = text,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // --- CELDA BENTO 4: Consola de Transito en Red Modular (Full Width) ---
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONSOLA DE TRÁNSITO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SecureGreen)
                        )
                        Text(
                            text = "LIVE FEED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = SecureGreen
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val displayLogs = logs.take(3)
                    if (displayLogs.isEmpty()) {
                        Text(
                            text = "[14:02:11] Init: TeleGuard Core v1.2",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = SecureGreen.copy(alpha = 0.7f)
                        )
                    } else {
                        displayLogs.forEach { log ->
                            Text(
                                text = log,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (log.contains("Error") || log.contains("Atención")) WarningOrange else SecureGreen.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatRoomRow(
    room: ChatRoom,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) SlateSurfaceVariant else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chat Avatar (Telegram Circle badge style)
        val initials = room.name.take(2).uppercase()
        val baseColor = try {
            Color(android.graphics.Color.parseColor(room.systemAvatarColorHex))
        } catch (e: Exception) {
            TelegramBlue
        }
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(baseColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Chat info
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = room.name,
                        color = TextPrimary,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    // Encryption indicator badge in Chats list
                    when (room.defaultEncryptionType) {
                        "ECDH-AES" -> {
                            Badge(
                                containerColor = SecureGreen,
                                contentColor = SlateDarkBg,
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Text("ECDH-AES", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        "GPG-RSA" -> {
                            Badge(
                                containerColor = SecureGreen,
                                contentColor = SlateDarkBg,
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Text("GPG-RSA", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {
                            Badge(
                                containerColor = BorderColor,
                                contentColor = TextSecondary,
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Text("PLAIN", fontSize = 8.sp)
                            }
                        }
                    }
                }
                
                Text(
                    text = "Ahora",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (room.defaultEncryptionType != "PLAIN") {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Secured",
                        tint = SecureGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (room.isGroup) "Mensajes grupales protegidos" else "Pulsa para enviar mensajes con capa criptográfica",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// --- ACTIVE CHAT PANEL UX ---

@Composable
fun ActiveChatPanel(
    viewModel: TeleViewModel,
    room: ChatRoom,
    onBackClick: () -> Unit,
    showBack: Boolean,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val activeInput by viewModel.messageInput.collectAsState()
    val selectedEncType by viewModel.selectedEncryptionType.collectAsState()
    val attachedMedia by viewModel.attachedMedia.collectAsState()
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Automatically scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .background(SlateDarkBg)
    ) {
        // Chat Header with keys indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateSurface)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = TextPrimary
                    )
                }
            }

            // Small Contact Avatar
            val initials = room.name.take(2).uppercase()
            val baseColor = try {
                Color(android.graphics.Color.parseColor(room.systemAvatarColorHex))
            } catch (e: Exception) {
                TelegramBlue
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(baseColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (room.activeContactId != 0) SecureGreen else WarningOrange)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = when {
                            room.isGroup -> "Grupo Seguro (Canal AES)"
                            room.activeContactId != 0 && room.name.contains("Pavel") -> "Contacto Estándar (No cifrado)"
                            room.activeContactId != 0 -> "Socio Seguro (ECDH/GPG)"
                            else -> "Canal Cifrado"
                        },
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Action triggers (trash for clean history)
            IconButton(
                onClick = { viewModel.clearAllMessagesOfActiveRoom() },
                modifier = Modifier.testTag("clear_chat_button")
            ) {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = "Borrar historial",
                    tint = TextSecondary
                )
            }
        }

        // Warning bar if destination has NO cryptographic capabilities (such as Pavel Durov)
        val isSecureContact = !room.name.contains("Pavel") && !room.isGroup
        if (!isSecureContact && !room.isGroup) {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Aviso no encriptación",
                        tint = WarningOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Este usuario utiliza el cliente regular de Telegram. Debe usar el modo 'TEXTO PLANO' para comunicarse de forma legible.",
                        fontSize = 10.sp,
                        color = WarningOrange,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        // Message stream scroll area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .testTag("messages_list"),
            contentPadding = PaddingValues(top = 16.dp, bottom = 12.dp)
        ) {
            items(messages) { message ->
                MessageBubbleRow(message = message)
            }
        }

        // Optional media attachment preview
        if (attachedMedia != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateSurfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (attachedMedia!!.second == "image") Icons.Default.Image else Icons.Default.Description,
                        contentDescription = "Media Attached",
                        tint = SecureGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            attachedMedia!!.first,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Archivo adjuntado y auto-cifrado antes de enviar",
                            fontSize = 9.sp,
                            color = SecureGreen
                        )
                    }
                }
                IconButton(onClick = { viewModel.clearAttachedMedia() }) {
                    Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = TextSecondary)
                }
            }
        }

        // Encryption Selector Hub (Modular interactive selectors - Diffie-Hellman EC + RSA)
        BottomCryptoSelectorBar(
            selectedEncType = selectedEncType,
            isSecureContact = isSecureContact,
            isGroup = room.isGroup,
            onTypeSelected = { viewModel.setEncryptionType(it) }
        )

        // Text entry / Send box
        BottomSendBoxRow(
            value = activeInput,
            onValueChange = { viewModel.updateMessageInput(it) },
            onSend = { viewModel.sendMessage() },
            onAttachFile = {
                // Simulate attachment toggle of secure images/docs
                viewModel.attachMedia("reporte_fiscal_confidencial.pdf", "document")
            },
            onAttachImage = {
                viewModel.attachMedia("captura_cripto_llaves.png", "image")
            },
            selectedEncType = selectedEncType
        )
    }
}

@Composable
fun BottomCryptoSelectorBar(
    selectedEncType: String,
    isSecureContact: Boolean,
    isGroup: Boolean,
    onTypeSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateSurfaceVariant)
            .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(0.dp))
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "MÉTODO DE CIFRADO:",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = TextSecondary,
            letterSpacing = 1.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Option 1: ECDH-AES (Diffie-Hellman)
            CryptoTypeButton(
                label = "ECDH-AES",
                icon = Icons.Default.Lock,
                selected = selectedEncType == "ECDH-AES",
                enabled = isSecureContact || isGroup,
                onClick = { onTypeSelected("ECDH-AES") }
            )

            // Option 2: GPG-RSA Envelope
            CryptoTypeButton(
                label = "GPG-RSA",
                icon = Icons.Default.Policy,
                selected = selectedEncType == "GPG-RSA",
                enabled = isSecureContact && !isGroup, // GPG-RSA is usually asymmetric unicast
                onClick = { onTypeSelected("GPG-RSA") }
            )

            // Option 3: Plain (no crypt fallback for normal clients)
            CryptoTypeButton(
                label = "TEXTO PLANO",
                icon = Icons.Default.NoEncryption,
                selected = selectedEncType == "PLAIN",
                enabled = true,
                onClick = { onTypeSelected("PLAIN") }
            )
        }
    }
}

@Composable
fun CryptoTypeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (!enabled) SlateSurface.copy(alpha = 0.5f)
                else if (selected) SecureGreen else SlateSurface
            )
            .border(
                width = 1.dp,
                color = if (selected) SecureGreen else BorderColor,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(10.dp),
                tint = if (!enabled) TextSecondary.copy(alpha = 0.3f) else if (selected) SlateDarkBg else TextPrimary
            )
            Text(
                label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (!enabled) TextSecondary.copy(alpha = 0.3f) else if (selected) SlateDarkBg else TextPrimary
            )
        }
    }
}

@Composable
fun MessageBubbleRow(message: Message) {
    val isMe = message.senderId == "me"
    
    // Bubble Alignment
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start, 
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            // Sender Identity info
            if (!isMe) {
                Text(
                    message.senderName,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            // Message Bubble Card
            val hasEnc = message.isEncrypted
            val borderModifier = if (hasEnc) {
                Modifier.border(
                    width = 1.dp, 
                    color = SecureGreen.copy(alpha = 0.6f), 
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isMe) 12.dp else 2.dp,
                        bottomEnd = if (isMe) 2.dp else 12.dp
                    )
                )
            } else Modifier

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isMe) {
                        if (hasEnc) SecureGreenDim.copy(alpha = 0.45f) else TelegramBlue.copy(alpha = 0.85f)
                    } else {
                        SlateSurface
                    }
                ),
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (isMe) 12.dp else 2.dp,
                    bottomEnd = if (isMe) 2.dp else 12.dp
                ),
                modifier = borderModifier
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    
                    // Show media content if present
                    if (message.mediaUrl != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SlateDarkBg)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (message.mediaType == "image") Icons.Default.Image else Icons.Default.Description,
                                contentDescription = "Media Attached",
                                tint = SecureGreen,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    message.mediaUrl,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    "Adjunto Cifrado (${message.encryptionType})",
                                    fontSize = 8.sp,
                                    color = SecureGreen
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Main Text Content
                    Text(
                        text = message.contentText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Small indicator block tracking Decryption / Algorithm status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasEnc) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Locks icon",
                                tint = SecureGreen,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Desencriptado (${message.encryptionType})",
                                fontSize = 8.sp,
                                color = SecureGreen,
                                fontWeight = FontWeight.Black
                            )
                        } else {
                            Text(
                                text = "Texto Claro (Sin Cifrar)",
                                fontSize = 8.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Real-time expansion to view the exact Raw Over-the-air base64 Telegram wire data!!
            if (hasEnc) {
                var showWirePayload by remember { mutableStateOf(false) }
                
                Text(
                    text = if (showWirePayload) "▲ Ocultar transito del cable (OTA)" else "▼ Ver tránsito real de red (Wire payload)",
                    fontSize = 9.sp,
                    color = SecureGreen.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { showWirePayload = !showWirePayload }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )

                if (showWirePayload) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateDarkBg.copy(alpha = 0.8f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(6.dp))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "Mando de Red (Lo que interceptaría un tercero o Telegram Corp):",
                                fontSize = 8.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.rawPayloadText,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = SecureGreen,
                                lineHeight = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomSendBoxRow(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachFile: () -> Unit,
    onAttachImage: () -> Unit,
    selectedEncType: String
) {
    val focusManager = LocalFocusManager.current
    var inputExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateSurface)
            .windowInsetsPadding(WindowInsets.ime)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expandable actions menu (+)
        IconButton(
            onClick = { inputExpanded = !inputExpanded },
            modifier = Modifier
                .clip(CircleShape)
                .background(SlateSurfaceVariant)
        ) {
            Icon(
                if (inputExpanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = "Adjuntos",
                tint = if (inputExpanded) WarningOrange else TextPrimary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (inputExpanded) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // File trigger
                IconButton(
                    onClick = {
                        onAttachFile()
                        inputExpanded = false
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SecureGreenDim)
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Archivo Seg", tint = SecureGreen)
                }

                // Image trigger
                IconButton(
                    onClick = {
                        onAttachImage()
                        inputExpanded = false
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SecureGreenDim)
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Captura Seg", tint = SecureGreen)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Custom stylized Input text Box
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Escribe un mensaje de TeleGuard...", fontSize = 13.sp, color = TextSecondary) },
            modifier = Modifier
                .weight(1f)
                .testTag("message_input_field"),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SlateDarkBg,
                unfocusedContainerColor = SlateDarkBg,
                disabledContainerColor = SlateDarkBg,
                focusedBorderColor = if (selectedEncType != "PLAIN") SecureGreen else TelegramBlue,
                unfocusedBorderColor = BorderColor,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                onSend()
                focusManager.clearFocus()
            }),
            maxLines = 4
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Dynamic colored Send Action icon (grows SecureGreen if encrypted, TelegramBlue if plaintext)
        IconButton(
            onClick = {
                onSend()
                focusManager.clearFocus()
            },
            enabled = value.isNotEmpty() || inputExpanded,
            modifier = Modifier
                .clip(CircleShape)
                .background(if (selectedEncType != "PLAIN") SecureGreen else TelegramBlue)
                .testTag("send_button")
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Enviar",
                tint = SlateDarkBg,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// --- MY KEYS ROUTE ---

@Composable
fun MyKeysStudioScreen(
    viewModel: TeleViewModel,
    modifier: Modifier = Modifier
) {
    val myKeys by viewModel.myKeys.collectAsState()
    var selectedViewKeyType by remember { mutableStateOf("GPG-RSA") } // "GPG-RSA" or "ECDH-EC"

    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .background(SlateDarkBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Estudio de Claves",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    "Identidades criptográficas soberanas de mi terminal",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
            
            Button(
                onClick = { viewModel.rotateSovereignKeys() },
                colors = ButtonDefaults.buttonColors(containerColor = SecureGreen, contentColor = SlateDarkBg),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("rotate_keys_btn")
            ) {
                Icon(Icons.Default.Autorenew, contentDescription = "Rotar", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Rotar Llaves", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Brief Security advisory notice
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = "Secure Keypair advice",
                    tint = SecureGreen,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Resguardo Descentralizado",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Tus claves privadas nunca se transmiten ni guardan en ningún servidor de Telegram. Se resguardan con sandboxing de seguridad en SQLite local.",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        // Segmented selector for key type viewing
        TabRow(
            selectedTabIndex = if (selectedViewKeyType == "GPG-RSA") 0 else 1,
            containerColor = SlateSurface,
            contentColor = SecureGreen,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .fillMaxWidth()
        ) {
            Tab(
                selected = selectedViewKeyType == "GPG-RSA",
                onClick = { selectedViewKeyType = "GPG-RSA" },
                text = { Text("GPG-RSA (2048 Bit)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedViewKeyType == "ECDH-EC",
                onClick = { selectedViewKeyType = "ECDH-EC" },
                text = { Text("ECDH-EC (Curv 256)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Get key for display
        val currentDisplayKey = myKeys.find { it.type == selectedViewKeyType }

        if (currentDisplayKey == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SecureGreen)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f)
            ) {
                // Key info summary card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Huella Digital de Llave",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Badge(containerColor = SecureGreen, contentColor = SlateDarkBg) {
                                    Text("ACTIVA", fontSize = 8.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentDisplayKey.keyId,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Monospace,
                                color = SecureGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BorderColor)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("GENERACIÓN", fontSize = 9.sp, color = TextSecondary)
                                    Text("Local SQLite", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("TIPO DE ALGORITMO", fontSize = 9.sp, color = TextSecondary)
                                    Text(
                                        if (selectedViewKeyType == "GPG-RSA") "RSA / PKCS1 Padding" else "ECDH secp256r1",
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Public Key PEM section
                item {
                    Text(
                        "LLAVE PÚBLICA (Public Key PEM)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 6.dp),
                        letterSpacing = 1.sp
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = currentDisplayKey.publicKeyPem,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                                lineHeight = 11.sp
                            )
                        }
                    }
                }

                // Space divider
                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Safe Private Key warning & indicator (satisfying UI craft)
                item {
                    Text(
                        "LLAVE PRIVADA (Sovereign Secret Key PEM)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 6.dp),
                        letterSpacing = 1.sp
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(width = 1.dp, color = WarningOrange.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LockOpen, contentDescription = "Sovereign key warning", tint = WarningOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("OCULTA POR SEGURIDAD", fontSize = 10.sp, color = WarningOrange, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "El motor TeleGuard mantiene esta llave encriptada con cifrado simétrico en su sandbox local. La llave privada se utiliza internamente para calcular la llave simétrica en las transacciones ECDH.",
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
                
                // Bottom space
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

// --- PLUGINS & MODIFICATIONS ROUTE ---

@Composable
fun PluginsCenterScreen(
    viewModel: TeleViewModel,
    modifier: Modifier = Modifier
) {
    val plugins by viewModel.plugins.collectAsState()
    val logs by viewModel.engineLogs.collectAsState()
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .background(SlateDarkBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Centro de Modificaciones",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    "Arquitectura modular de plugins e interceptores",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Quick developer documentation regarding plugins
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = "Dev Code", tint = SecureGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guía para Desarrolladores", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Puedes extender la clase base de interceptores escribiendo tus filtros personalizados. Las salidas decodificadas o de envío se canalizan secuencialmente por esta tubería móvil.",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    lineHeight = 13.sp
                )
            }
        }

        // Plugins List
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1.2f)
        ) {
            item {
                Text(
                    "PLUGINS E INTERCEPTORES CARGADOS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp),
                    letterSpacing = 1.sp
                )
            }

            items(plugins) { plugin ->
                PluginRow(
                    plugin = plugin,
                    onToggleEnabled = { enabled ->
                        viewModel.togglePluginState(plugin.id, enabled)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Console Logs (Real-time cryptographic wire events output!)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CONSOLA EN TIEMPO REAL (TeleGuard Core)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SecureGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LIVE FEED", fontSize = 9.sp, color = SecureGreen, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Console visual Box
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                modifier = Modifier
                    .fillMaxSize()
                    .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(8.dp))
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (logs.isEmpty()) {
                        item {
                            Text(
                                "Esperando transacciones de red...",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        }
                    } else {
                        items(logs) { log ->
                            Text(
                                text = log,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (log.contains("Error") || log.contains("Atención")) WarningOrange else SecureGreen,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PluginRow(
    plugin: Plugin,
    onToggleEnabled: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    plugin.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    plugin.description,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                    lineHeight = 13.sp
                )
                Text(
                    "Autor: ${plugin.author}",
                    fontSize = 8.sp,
                    color = TelegramBlue,
                    modifier = Modifier.padding(top = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            
            Switch(
                checked = plugin.isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SlateDarkBg,
                    checkedTrackColor = SecureGreen,
                    uncheckedThumbColor = SlateSurfaceVariant,
                    uncheckedTrackColor = BorderColor
                ),
                modifier = Modifier.testTag("plugin_switch_${plugin.id}")
            )
        }
    }
}

// --- DIALOGS ---

@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onAddContact: (String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isSecureClient by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(width = 1.dp, color = BorderColor, shape = RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    "Nuevo Socio de Chat",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Socio", color = TextSecondary) },
                    placeholder = { Text("Ej. Alicia", color = TextSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_contact_name_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SlateDarkBg,
                        unfocusedContainerColor = SlateDarkBg,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = SecureGreen,
                        unfocusedBorderColor = BorderColor
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Biografía / Info", color = TextSecondary) },
                    placeholder = { Text("Pares criptográficos...", color = TextSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_contact_bio_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SlateDarkBg,
                        unfocusedContainerColor = SlateDarkBg,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = SecureGreen,
                        unfocusedBorderColor = BorderColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Secured Peer Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateSurfaceVariant)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "¿Usa TeleGuard?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Indica si posee soporte para cifrado asimétrico GPG/ECDH.",
                            fontSize = 9.sp,
                            color = TextSecondary,
                            lineHeight = 11.sp
                        )
                    }
                    Switch(
                        checked = isSecureClient,
                        onCheckedChange = { isSecureClient = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SlateDarkBg,
                            checkedTrackColor = SecureGreen,
                            uncheckedThumbColor = SlateSurfaceVariant,
                            uncheckedTrackColor = BorderColor
                        ),
                        modifier = Modifier.testTag("dialog_secure_switch")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (name.trim().isNotEmpty()) {
                                onAddContact(name.trim(), bio.trim().ifEmpty { "Socio activo de TeleGuard." }, isSecureClient)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecureGreen, contentColor = SlateDarkBg),
                        enabled = name.trim().isNotEmpty(),
                        modifier = Modifier.testTag("dialog_add_button")
                    ) {
                        Text("Añadir", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
