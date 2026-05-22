package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CryptoEngine
import com.example.db.TeleRepository
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.crypto.spec.SecretKeySpec

class TeleViewModel(application: Application) : AndroidViewModel(application) {

    val repository = TeleRepository(application)
    
    // UI State Holders
    val contacts = repository.allContacts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val chatRooms = repository.allChatRooms.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val myKeys = repository.allMyKeys.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val plugins = repository.allPlugins.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedChatRoom = MutableStateFlow<ChatRoom?>(null)
    val selectedChatRoom: StateFlow<ChatRoom?> = _selectedChatRoom

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    // Composition State
    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput

    private val _selectedEncryptionType = MutableStateFlow("ECDH-AES") // "ECDH-AES", "GPG-RSA", "PLAIN"
    val selectedEncryptionType: StateFlow<String> = _selectedEncryptionType

    // Attachment State (Group and Media support)
    private val _attachedMedia = MutableStateFlow<Pair<String, String>?>(null) // Pair(Name, Type) e.g., Pair("image_receipt.jpg", "image")
    val attachedMedia: StateFlow<Pair<String, String>?> = _attachedMedia

    // Dynamic Engine Logs for verification of the Plugin interceptor architecture
    private val _engineLogs = MutableStateFlow<List<String>>(
        listOf("Sistema TeleGuard inicializado correctamente.", "Motores criptográficos listos para GPG-RSA y ECDH-256.")
    )
    val engineLogs: StateFlow<List<String>> = _engineLogs

    init {
        viewModelScope.launch {
            // Seed DB with demo data & generate initial system identities
            repository.generateAndSaveOwnKeys()
            repository.seedDatabaseIfEmpty()
            
            // Set first room as default selected
            val initialRooms = repository.allChatRooms.first()
            if (initialRooms.isNotEmpty()) {
                selectChatRoom(initialRooms.first())
            }
        }
    }

    fun selectChatRoom(room: ChatRoom) {
        _selectedChatRoom.value = room
        // Reset inputs and load messages
        _messageInput.value = ""
        _attachedMedia.value = null
        _selectedEncryptionType.value = room.defaultEncryptionType

        // Observe messages of this room
        viewModelScope.launch {
            repository.getMessagesForRoom(room.id).collect { roomMessages ->
                _messages.value = roomMessages
            }
        }
        viewModelScope.launch {
            repository.dao.clearUnreads(room.id)
        }
        logEngine("Sala de chat cambiada a: ${room.name}. Algoritmo de envío sugerido: ${room.defaultEncryptionType}")
    }

    fun updateMessageInput(input: String) {
        _messageInput.value = input
    }

    fun setEncryptionType(type: String) {
        _selectedEncryptionType.value = type
        logEngine("Algoritmo de envío modificado manualmente a: $type")
    }

    fun attachMedia(name: String, type: String) {
        _attachedMedia.value = Pair(name, type)
        logEngine("Archivo multimedia adjuntado: $name ($type)")
    }

    fun clearAttachedMedia() {
        _attachedMedia.value = null
        logEngine("Archivo multimedia removido.")
    }

    fun logEngine(log: String) {
        val currentLogs = _engineLogs.value.toMutableList()
        currentLogs.add(0, "[${System.currentTimeMillis() % 100000}] $log")
        _engineLogs.value = currentLogs.take(30)
    }

    // --- Message Interceptors (MODULAR PLUGIN SYSTEM) ---

    private suspend fun runSendInterceptors(rawText: String): String {
        var processed = rawText
        val activePlugins = repository.dao.getActivePlugins()

        for (plugin in activePlugins) {
            when (plugin.id) {
                "emoji-booster" -> {
                    // Visual formatting helper
                    processed = "⚡ $processed"
                    logEngine("Plugin '${plugin.name}' interceptó envío: Agregó indicador visual.")
                }
                "censor-spam" -> {
                    // Anti-spam simulation
                    val clean = processed.replace("compra", "c*****").replace("bitcoin", "b******")
                    if (clean != processed) {
                        processed = clean
                        logEngine("Plugin '${plugin.name}' sanitizó el mensaje antes de cifrar.")
                    }
                }
            }
        }
        return processed
    }

    fun sendMessage() {
        val textToSend = _messageInput.value.trim()
        val media = _attachedMedia.value
        
        if (textToSend.isEmpty() && media == null) return

        val room = _selectedChatRoom.value ?: return
        val encType = _selectedEncryptionType.value

        viewModelScope.launch {
            logEngine("Iniciando tubería de envío para '${textToSend.take(15)}...' con método: $encType")
            
            // 1. Run modular plugins before encryption
            val preProcessedText = if (textToSend.isNotEmpty()) {
                runSendInterceptors(textToSend)
            } else {
                ""
            }

            var finalContentText = preProcessedText
            var rawWirePayload = preProcessedText
            var isEncrypted = false

            // Get target contact details if private
            val contact = if (room.activeContactId != 0) {
                repository.dao.getContactById(room.activeContactId)
            } else null

            // 2. Encryption layer configuration
            if (encType != "PLAIN") {
                if (room.isGroup) {
                    // Group context: Simulate shared group secret key agreement
                    logEngine("Detectado entorno grupal. Derivando llave AES del grupo...")
                    val sampleKey = SecretKeySpec("TeleguardGroupSecretHashKeyShared".toByteArray().take(16).toByteArray(), "AES")
                    val cipherText = CryptoEngine.encryptAes(preProcessedText, sampleKey)
                    rawWirePayload = "-----BEGIN TELEGUARD CIPHERBLOCK (GROUP-AES)-----\nPayload: $cipherText\n-----END TELEGUARD CIPHERBLOCK-----"
                    isEncrypted = true
                    logEngine("Cifrado Simétrico AES grupal completado exitosamente.")
                } else if (contact != null) {
                    if (contact.isSecureClient) {
                        when (encType) {
                            "ECDH-AES" -> {
                                logEngine("Obteniendo llaves de curva elíptica...")
                                val myEcKey = repository.dao.getKeyByType("ECDH-EC")
                                if (myEcKey != null && contact.ecPublicKeyPem.isNotEmpty()) {
                                    val sharedAesKey = CryptoEngine.computeEcdhSharedSecret(myEcKey.privateKeyPem, contact.ecPublicKeyPem)
                                    val cipherText = CryptoEngine.encryptAes(preProcessedText, sharedAesKey)
                                    rawWirePayload = "-----BEGIN TELEGUARD CIPHERBLOCK (ECDH-AES)-----\nPayload: $cipherText\n-----END TELEGUARD CIPHERBLOCK-----"
                                    isEncrypted = true
                                    logEngine("Acuerdo de claves ECDH resuelto. Secreto derivado SHA-256 AES aplicado.")
                                } else {
                                    logEngine("Error: Faltan llaves EC para ECDH. Cayendo a plaintext.")
                                }
                            }
                            "GPG-RSA" -> {
                                logEngine("Cargando sobre GPG/RSA con llave pública del receptor...")
                                if (contact.rsaPublicKeyPem.isNotEmpty()) {
                                    val cipherText = CryptoEngine.encryptRsa(preProcessedText, contact.rsaPublicKeyPem)
                                    rawWirePayload = "-----BEGIN PGP MESSAGE-----\nVersion: TeleGuard GPG v1.0\n\n$cipherText\n-----END PGP MESSAGE-----"
                                    isEncrypted = true
                                    logEngine("Sobre asimétrico GPG-RSA creado exitosamente.")
                                } else {
                                    logEngine("Error: Falta llave RSA del receptor.")
                                }
                            }
                        }
                    } else {
                        logEngine("Atención: El destinatario usa cliente oficial de Telegram. Forzando envío sin encriptar.")
                        rawWirePayload = preProcessedText
                        isEncrypted = false
                    }
                } else {
                    // Fallback plain
                    rawWirePayload = preProcessedText
                }
            } else {
                logEngine("Envío explícito en texto plano (sin encriptación).")
            }

            // Create sent message
            val msg = Message(
                chatRoomId = room.id,
                senderId = "me",
                senderName = "Yo",
                contentText = textToSend, // Keep input as original readable
                rawPayloadText = rawWirePayload,
                isEncrypted = isEncrypted,
                encryptionType = if (isEncrypted) encType else "None",
                mediaUrl = media?.first,
                mediaType = media?.second,
                isIncoming = false
            )

            repository.insertMessage(msg)
            _messageInput.value = ""
            _attachedMedia.value = null

            // Simulate incoming response after a short delay
            simulateIncomingResponse(room, textToSend, encType)
        }
    }

    private fun simulateIncomingResponse(room: ChatRoom, lastSent: String, encType: String) {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            
            // Get target contact details if private
            val contact = if (room.activeContactId != 0) {
                repository.dao.getContactById(room.activeContactId)
            } else null

            val senderName = contact?.name ?: "Socio Seguro"
            val senderId = contact?.id?.toString() ?: "99"

            val isPlaintextResponse = contact?.isSecureClient == false || encType == "PLAIN"

            val responseText = when {
                isPlaintextResponse -> {
                    "Recibí tu mensaje en claro. ¡Gracias por no encriptar esta vez, mi cliente oficial de Telegram no soporta claves GPG!"
                }
                lastSent.lowercase().contains("hola") || lastSent.lowercase().contains("saludos") -> {
                    "¡Un saludo seguro! ¿Estás probando la capa criptográfica ECDH?"
                }
                lastSent.lowercase().contains("grupo") -> {
                    "Soportamos perfectamente media y grupos. El canal distribuye la clave compartida de forma transparente."
                }
                else -> {
                    "Mensaje procesado y desencriptado con éxito en mi terminal. La firma digital coincide."
                }
            }

            var rawPayload = responseText
            var isEncResult = false

            if (!isPlaintextResponse) {
                isEncResult = true
                if (encType == "ECDH-AES" && contact != null) {
                    val myEcKey = repository.dao.getKeyByType("ECDH-EC")
                    if (myEcKey != null) {
                        val sharedAesKey = CryptoEngine.computeEcdhSharedSecret(myEcKey.privateKeyPem, contact.ecPublicKeyPem)
                        val cipherText = CryptoEngine.encryptAes(responseText, sharedAesKey)
                        rawPayload = "-----BEGIN TELEGUARD CIPHERBLOCK (ECDH-AES)-----\nPayload: $cipherText\n-----END TELEGUARD CIPHERBLOCK-----"
                    }
                } else if (encType == "GPG-RSA" && contact != null) {
                    // Receiver encrypts with MY public key, so I can decrypt it
                    val myRsaKey = repository.dao.getKeyByType("GPG-RSA")
                    if (myRsaKey != null) {
                        val cipherText = CryptoEngine.encryptRsa(responseText, myRsaKey.publicKeyPem)
                        rawPayload = "-----BEGIN PGP MESSAGE-----\nVersion: TeleGuard GPG v1.0\n\n$cipherText\n-----END PGP MESSAGE-----"
                    }
                } else {
                    // Group context fallback
                    val sampleKey = SecretKeySpec("TeleguardGroupSecretHashKeyShared".toByteArray().take(16).toByteArray(), "AES")
                    val cipherText = CryptoEngine.encryptAes(responseText, sampleKey)
                    rawPayload = "-----BEGIN TELEGUARD CIPHERBLOCK (GROUP-AES)-----\nPayload: $cipherText\n-----END TELEGUARD CIPHERBLOCK-----"
                }
            }

            logEngine("Recibiendo payload cifrado desde '$senderName' via red de Telegram...")
            
            // Simulation of receiving and automatic decoding process on TeleGuard
            val msg = Message(
                chatRoomId = room.id,
                senderId = senderId,
                senderName = senderName,
                contentText = responseText, // Directly decrypted by TeleGuard app
                rawPayloadText = rawPayload,
                isEncrypted = isEncResult,
                encryptionType = if (isEncResult) encType else "None",
                isIncoming = true
            )
            
            repository.insertMessage(msg)
            logEngine("Mensaje de '$senderName' desencriptado y renderizado automáticamente en chat.")
        }
    }

    fun togglePluginState(pluginId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.togglePlugin(pluginId, isEnabled)
            logEngine("Plugin '$pluginId' cambiado a: ${if (isEnabled) "ACTIVADO" else "DESACTIVADO"}")
        }
    }

    fun addNewContact(name: String, bio: String, isSecure: Boolean) {
        viewModelScope.launch {
            val rsaPair = CryptoEngine.generateRsaKeyPair()
            val ecPair = CryptoEngine.generateEcKeyPair()
            
            val contact = Contact(
                name = name,
                bio = bio,
                avatarColorHex = listOf("#FF00E676", "#FFFF9100", "#FF1769AA", "#FFD500F9", "#FF00B0FF").random(),
                isSecureClient = isSecure,
                rsaPublicKeyPem = if (isSecure) rsaPair.publicKeyPem else "",
                ecPublicKeyPem = if (isSecure) ecPair.publicKeyPem else ""
            )
            
            val contactId = repository.dao.insertContact(contact).toInt()
            
            // Automatically make active chatroom
            val room = ChatRoom(
                name = name,
                isGroup = false,
                unreadCount = 0,
                systemAvatarColorHex = contact.avatarColorHex,
                defaultEncryptionType = if (isSecure) "ECDH-AES" else "PLAIN",
                activeContactId = contactId
            )
            
            val roomId = repository.dao.insertChatRoom(room).toInt()
            val createdRoom = room.copy(id = roomId)
            
            // Seed a welcome/info msg
            repository.dao.insertMessage(Message(
                chatRoomId = roomId,
                senderId = "me",
                senderName = "Yo",
                contentText = "Contacto '$name' añadido de forma segura.",
                rawPayloadText = "System Message: Contact added",
                isEncrypted = false,
                isIncoming = false
            ))
            
            selectChatRoom(createdRoom)
            logEngine("Nuevo contacto '$name' añadido con éxito. Intercambio de llaves automáticas.")
        }
    }

    fun rotateSovereignKeys() {
        viewModelScope.launch {
            logEngine("Iniciando rotación soberana de llaves criptográficas...")
            
            val rsaPair = CryptoEngine.generateRsaKeyPair()
            val ecPair = CryptoEngine.generateEcKeyPair()
            
            val shortRsaFingerprint = rsaPair.publicKeyPem.hashCode().toUInt().toString(16).take(8)
            val shortEcFingerprint = ecPair.publicKeyPem.hashCode().toUInt().toString(16).take(8)
            
            repository.dao.insertCryptoKey(CryptoKey(
                type = "GPG-RSA",
                publicKeyPem = rsaPair.publicKeyPem,
                privateKeyPem = rsaPair.privateKeyPem,
                keyId = "0x$shortRsaFingerprint",
                timestampGenerated = System.currentTimeMillis()
            ))

            repository.dao.insertCryptoKey(CryptoKey(
                type = "ECDH-EC",
                publicKeyPem = ecPair.publicKeyPem,
                privateKeyPem = ecPair.privateKeyPem,
                keyId = "0x$shortEcFingerprint",
                timestampGenerated = System.currentTimeMillis()
            ))

            logEngine("Rotación exitosa. Nuva huella RSA: 0x$shortRsaFingerprint, Nueva huella ECDH: 0x$shortEcFingerprint. Anunciando a pares...")
        }
    }

    fun clearAllMessagesOfActiveRoom() {
        val room = _selectedChatRoom.value ?: return
        viewModelScope.launch {
            repository.clearMessages(room.id)
            logEngine("Histórico de mensajes borrado para la sala: ${room.name}")
        }
    }
}
