package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val bio: String = "No bio",
    val avatarColorHex: String = "#FF0088CC", // Standard Telegram Blue
    val isSecureClient: Boolean = true, // Whether they use TeleGuard or a normal client
    val rsaPublicKeyPem: String = "", // GPG representing key
    val ecPublicKeyPem: String = "" // ECDH representing key
)

@Entity(tableName = "chat_rooms")
data class ChatRoom(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isGroup: Boolean = false,
    val unreadCount: Int = 0,
    val systemAvatarColorHex: String = "#FF0088CC",
    val defaultEncryptionType: String = "ECDH-AES", // ECDH-AES, GPG-RSA, or PLAIN
    val activeContactId: Int = 0 // Pointing to Contact if private chat
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val chatRoomId: Int,
    val senderId: String, // "me" or dynamic id string
    val senderName: String,
    val contentText: String, // Decrypted/Readable content
    val rawPayloadText: String, // Actual raw message passed over "Telegram wire" (ciphertext/PEM)
    val timestamp: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = false,
    val encryptionType: String = "None", // "None", "GPG-RSA", "ECDH-AES"
    val mediaUrl: String? = null, // Path or representative name of media (for group/media support)
    val mediaType: String? = null, // "image", "document", "voice", null
    val isIncoming: Boolean = true
)

@Entity(tableName = "crypto_keys")
data class CryptoKey(
    @PrimaryKey val type: String, // "GPG-RSA", "ECDH-EC"
    val publicKeyPem: String,
    val privateKeyPem: String,
    val keyId: String = "", // Fingerprint or short hash
    val isActive: Boolean = true,
    val timestampGenerated: Long = System.currentTimeMillis()
)

@Entity(tableName = "plugins")
data class Plugin(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean = false,
    val author: String = "TeleGuard Core",
    val type: String = "message_interceptor" // "message_interceptor", "ui_customization"
)
