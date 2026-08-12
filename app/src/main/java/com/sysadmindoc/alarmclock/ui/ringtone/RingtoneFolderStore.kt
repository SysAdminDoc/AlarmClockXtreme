package com.sysadmindoc.alarmclock.ui.ringtone

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

/**
 * Keeps the small amount of state needed for document-tree ringtone sources.
 * The selected tree URI is persisted only after Android grants read access;
 * no filesystem path or broad storage permission is required.
 */
internal object RingtoneFolderStore {
    private const val PREFERENCES = "ringtone_sources"
    private const val FOLDER_URIS = "folder_uris"

    private val audioExtensions = setOf(
        "3gp", "aac", "amr", "flac", "m4a", "mid", "midi", "mp3", "ogg", "opus", "wav"
    )

    fun addFolder(context: Context, uri: Uri) {
        val folders = folderUris(context).toMutableSet()
        folders += uri.toString()
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(FOLDER_URIS, folders)
            .apply()
    }

    fun folderUris(context: Context): Set<String> =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getStringSet(FOLDER_URIS, emptySet())
            .orEmpty()

    fun loadItems(context: Context): List<RingtoneItem> =
        folderUris(context)
            .flatMap { rawUri -> loadChildren(context, Uri.parse(rawUri)) }
            .distinctBy { it.uri }
            .sortedBy { it.title.lowercase() }

    internal fun isSupportedAudioDocument(displayName: String?, mimeType: String?): Boolean {
        if (mimeType?.startsWith("audio/", ignoreCase = true) == true) return true
        val extension = displayName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            .orEmpty()
        return extension in audioExtensions
    }

    private fun loadChildren(context: Context, treeUri: Uri): List<RingtoneItem> = runCatching {
        val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocumentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        val items = mutableListOf<RingtoneItem>()
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            if (idColumn < 0 || nameColumn < 0 || mimeColumn < 0) return@use

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn).orEmpty()
                val mimeType = cursor.getString(mimeColumn)
                if (!isSupportedAudioDocument(name, mimeType)) continue
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    cursor.getString(idColumn)
                )
                items += RingtoneItem(title = "$name (folder)", uri = documentUri.toString())
            }
        }
        items
    }.getOrDefault(emptyList())
}
