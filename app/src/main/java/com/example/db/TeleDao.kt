package com.example.db

import androidx.room.*
import com.example.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TeleDao {

    // --- Contacts DAO ---
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContactsFlow(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: Int): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact): Long

    @Delete
    suspend fun deleteContact(contact: Contact)

    // --- ChatRooms DAO ---
    @Query("SELECT * FROM chat_rooms ORDER BY id DESC")
    fun getAllChatRoomsFlow(): Flow<List<ChatRoom>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatRoom(chatRoom: ChatRoom): Long

    @Query("UPDATE chat_rooms SET unreadCount = 0 WHERE id = :chatRoomId")
    suspend fun clearUnreads(chatRoomId: Int)

    @Delete
    suspend fun deleteChatRoom(chatRoom: ChatRoom)

    // --- Messages DAO ---
    @Query("SELECT * FROM messages WHERE chatRoomId = :chatRoomId ORDER BY timestamp ASC")
    fun getMessagesForRoomFlow(chatRoomId: Int): Flow<List<Message>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessage(): Message?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Query("DELETE FROM messages WHERE chatRoomId = :chatRoomId")
    suspend fun clearMessagesForRoom(chatRoomId: Int)

    // --- CryptoKeys DAO ---
    @Query("SELECT * FROM crypto_keys WHERE type = :type LIMIT 1")
    suspend fun getKeyByType(type: String): CryptoKey?

    @Query("SELECT * FROM crypto_keys")
    fun getAllMyKeysFlow(): Flow<List<CryptoKey>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCryptoKey(key: CryptoKey)

    // --- Plugins DAO ---
    @Query("SELECT * FROM plugins")
    fun getAllPluginsFlow(): Flow<List<Plugin>>

    @Query("SELECT * FROM plugins WHERE isEnabled = 1")
    suspend fun getActivePlugins(): List<Plugin>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlugin(plugin: Plugin)

    @Query("UPDATE plugins SET isEnabled = :enabled WHERE id = :pluginId")
    suspend fun togglePlugin(pluginId: String, enabled: Boolean)
}
