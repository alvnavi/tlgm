package com.example.db

import android.content.Context
import com.example.crypto.CryptoEngine
import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TeleRepository(private val context: Context) {

    private val db = TeleDatabase.getDatabase(context)
    val dao = db.teleDao()

    // Flow feeds
    val allContacts: Flow<List<Contact>> = dao.getAllContactsFlow()
    val allChatRooms: Flow<List<ChatRoom>> = dao.getAllChatRoomsFlow()
    val allMyKeys: Flow<List<CryptoKey>> = dao.getAllMyKeysFlow()
    val allPlugins: Flow<List<Plugin>> = dao.getAllPluginsFlow()

    fun getMessagesForRoom(roomId: Int): Flow<List<Message>> {
        return dao.getMessagesForRoomFlow(roomId)
    }

    suspend fun insertMessage(message: Message): Long {
        return dao.insertMessage(message)
    }

    suspend fun clearMessages(roomId: Int) {
        dao.clearMessagesForRoom(roomId)
    }

    suspend fun togglePlugin(pluginId: String, enabled: Boolean) {
        dao.togglePlugin(pluginId, enabled)
    }

    suspend fun generateAndSaveOwnKeys() {
        // Generate and save RSA key pair if missing
        if (dao.getKeyByType("GPG-RSA") == null) {
            val rsaPair = CryptoEngine.generateRsaKeyPair()
            val shortRsaFingerprint = rsaPair.publicKeyPem.hashCode().toUInt().toString(16).take(8)
            dao.insertCryptoKey(CryptoKey(
                type = "GPG-RSA",
                publicKeyPem = rsaPair.publicKeyPem,
                privateKeyPem = rsaPair.privateKeyPem,
                keyId = "0x$shortRsaFingerprint"
            ))
        }

        // Generate and save EC key pair if missing
        if (dao.getKeyByType("ECDH-EC") == null) {
            val ecPair = CryptoEngine.generateEcKeyPair()
            val shortEcFingerprint = ecPair.publicKeyPem.hashCode().toUInt().toString(16).take(8)
            dao.insertCryptoKey(CryptoKey(
                type = "ECDH-EC",
                publicKeyPem = ecPair.publicKeyPem,
                privateKeyPem = ecPair.privateKeyPem,
                keyId = "0x$shortEcFingerprint"
            ))
        }
    }

    suspend fun seedDatabaseIfEmpty() {
        // 1. Check plugins
        val currentPlugins = dao.getAllPluginsFlow().first()
        if (currentPlugins.isEmpty()) {
            dao.insertPlugin(Plugin(
                id = "gpg-crypto",
                name = "Modulo PGP Autocrypt",
                description = "Gestiona y procesa de forma autonoma sobres de encriptacion PGP/RSA compatibles con clientes de escritorio.",
                isEnabled = true,
                author = "GNU Privacy Guard Client Suite"
            ))
            dao.insertPlugin(Plugin(
                id = "ecdh-aes",
                name = "Acuerdo de Claves ECDH-AES-GCM",
                description = "Deriva un secreto compartido efimero usando Elliptic Curve Diffie-Hellman y cifra los payloads mediante AES con IV dinamicos.",
                isEnabled = true,
                author = "TeleGuard Crypto Labs"
            ))
            dao.insertPlugin(Plugin(
                id = "emoji-booster",
                name = "Inyector de Emojis de Seguridad",
                description = "Intercepte la salida del chat y formatea los indicadores de cifrado de forma visual para destacar los canales seguros (🔒, 🔓).",
                isEnabled = true,
                author = "UX Modifiers"
            ))
            dao.insertPlugin(Plugin(
                id = "censor-spam",
                name = "Filtro de Censo y Sanitización",
                description = "Sanitiza y filtra strings fraudulentos o sospechosos en mensajes entrantes para prevenir phishing.",
                isEnabled = false,
                author = "SpamBuster Intl"
            ))
        }

        // 2. Check contacts
        val currentContacts = dao.getAllContactsFlow().first()
        if (currentContacts.isEmpty()) {
            // Generate simulated keys for secure contacts
            val rsaPairAlice = CryptoEngine.generateRsaKeyPair()
            val ecPairAlice = CryptoEngine.generateEcKeyPair()

            val rsaPairBob = CryptoEngine.generateRsaKeyPair()
            val ecPairBob = CryptoEngine.generateEcKeyPair()

            val aliceId = dao.insertContact(Contact(
                name = "Alicia (Socio TeleGuard, Seguro)",
                bio = "Cripto-aficionada. Claves RSA y ECDH activas y publicadas.",
                avatarColorHex = "#FF00B0FF", // Bright light blue
                isSecureClient = true,
                rsaPublicKeyPem = rsaPairAlice.publicKeyPem,
                ecPublicKeyPem = ecPairAlice.publicKeyPem
            )).toInt()

            val bobId = dao.insertContact(Contact(
                name = "Roberto (Socio TeleGuard, Seguro)",
                bio = "Prefiero comunicacion asincrona encriptada y canales efimeros.",
                avatarColorHex = "#FF00E676", // Light green
                isSecureClient = true,
                rsaPublicKeyPem = rsaPairBob.publicKeyPem,
                ecPublicKeyPem = ecPairBob.publicKeyPem
            )).toInt()

            val pavelId = dao.insertContact(Contact(
                name = "Pavel Durov (Cliente Telegram Estándar)",
                bio = "Digital nomad. Telegram Founder. No tienes mi clave de TeleGuard.",
                avatarColorHex = "#FF3F51B5", // Dark blue Telegram official style
                isSecureClient = false // Normal client!
            )).toInt()

            // 3. Create default ChatRooms
            val chat1 = dao.insertChatRoom(ChatRoom(
                name = "Alicia",
                isGroup = false,
                unreadCount = 0,
                systemAvatarColorHex = "#FF00B0FF",
                defaultEncryptionType = "ECDH-AES",
                activeContactId = aliceId
            )).toInt()

            val chat2 = dao.insertChatRoom(ChatRoom(
                name = "Roberto",
                isGroup = false,
                unreadCount = 0,
                systemAvatarColorHex = "#FF00E676",
                defaultEncryptionType = "GPG-RSA",
                activeContactId = bobId
            )).toInt()

            val chat3 = dao.insertChatRoom(ChatRoom(
                name = "Pavel Durov",
                isGroup = false,
                unreadCount = 0,
                systemAvatarColorHex = "#FF3F51B5",
                defaultEncryptionType = "PLAIN", // Plain by default, as Pavel has no keys
                activeContactId = pavelId
            )).toInt()

            val chatGroup = dao.insertChatRoom(ChatRoom(
                name = "🛡️ Grupo Criptográfico Organizado",
                isGroup = true,
                unreadCount = 0,
                systemAvatarColorHex = "#FF9C27B0", // Deep Purple
                defaultEncryptionType = "ECDH-AES",
                activeContactId = 0
            )).toInt()

            // Seed initial greeting messages
            dao.insertMessage(Message(
                chatRoomId = chat1,
                senderId = "me",
                senderName = "Yo",
                contentText = "Se realizó la conexión segura con GPG y Elliptic-Curves.",
                rawPayloadText = "Petición Handshake TeleGuard OK",
                isEncrypted = false,
                isIncoming = false,
                encryptionType = "None"
            ))

            // Seed introductory response from Alice
            // Alice's ECDH shared key simulation message
            dao.insertMessage(Message(
                chatRoomId = chat1,
                senderId = aliceId.toString(),
                senderName = "Alicia",
                contentText = "¡Hola! Estoy usando el plugin de cifrado ECDH-AES. Todo lo que me envíes usando esta opción se desencriptará automáticamente en mi móvil.",
                rawPayloadText = "-----BEGIN TELEGUARD CIPHERBLOCK (ECDH-AES)-----\nIV: dGVzdC1pdi12YWx1ZQ==\nPayload: Z29vZ2xlYnVpbGQtaGlnaC1wcmVjaXNpb24tc2VjdXJl\n-----END TELEGUARD CIPHERBLOCK-----",
                isEncrypted = true,
                isIncoming = true,
                encryptionType = "ECDH-AES"
            ))

            // Pavel warns about normal client fallback
            dao.insertMessage(Message(
                chatRoomId = chat3,
                senderId = pavelId.toString(),
                senderName = "Pavel Durov",
                contentText = "Hey! No tengo instalada la extensión TeleGuard en mi cuenta. Si me envías un mensaje cifrado, veré solamente el bloque de texto con el payload codificado en Base64.",
                rawPayloadText = "Hey! No tengo instalada la extensión TeleGuard...",
                isEncrypted = false,
                isIncoming = true,
                encryptionType = "None"
            ))
            
            // Seed a broadcast group message
            dao.insertMessage(Message(
                chatRoomId = chatGroup,
                senderId = aliceId.toString(),
                senderName = "Alicia",
                contentText = "Este es un grupo de prueba. Las claves se intercambian por encima de Telegram de manera segura con AES.",
                rawPayloadText = "-----BEGIN TELEGUARD GROUPPAYLOAD-----\nPayload: MDMyNDI4OTBhZHNmYWQyY2Ez\n-----END TELEGUARD GROUPPAYLOAD-----",
                isEncrypted = true,
                isIncoming = true,
                encryptionType = "ECDH-AES"
            ))
        }
    }
}
