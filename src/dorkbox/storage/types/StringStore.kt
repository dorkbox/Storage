/*
 * Copyright 2021 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.storage.types

import com.esotericsoftware.kryo.Kryo
import dorkbox.bytes.decodeBase58
import dorkbox.bytes.encodeToBase58String
import dorkbox.storage.AccessFunc
import dorkbox.storage.Storage
import dorkbox.storage.serializer.SerializerBytes
import mu.KLogger
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Java STRING file storage system
 */
abstract class StringStore(
    val dbFile: File,
    val autoLoad: Boolean,
    val readOnly: Boolean,
    val readOnlyViolent: Boolean,
    logger: KLogger,
    onNewKryo: Kryo.() -> Unit,
    onLoad: AccessFunc?,
    onSave: AccessFunc?,
) : Storage(logger) {

    companion object {
        internal val comparator = Comparator<Any> { o1, o2 -> o1.toString().compareTo(o2.toString()) }
    }

    private val thread = Thread { close() }

    @Volatile
    private var lastModifiedTime = 0L

    @Volatile
    private var isDirty = false

    protected val onLoadFunc: AccessFunc
    private val onSaveFunc: AccessFunc
    protected val loadFunc: (key: Any, value: Any) -> Unit
    protected val saveFunc: (key: Any, value: Any?) -> Unit

    private val loadedProps = ConcurrentHashMap<Any, Any>()
    private val serializer = SerializerBytes(onNewKryo)

    init {
        loadFunc = { key, value ->
            loadedProps[key] = value
        }
        saveFunc = { key, value ->
            onSave(key, value)
        }

        onLoadFunc = if (onLoad != null) {
            onLoad
        } else {
            { key, value, load ->
                val keyBytes = (key as String).decodeBase58()
                val valueBytes = (value as String).decodeBase58()

                val xKey = serializer.deserialize<Any>(keyBytes)!!
                val xValue = serializer.deserialize<Any>(valueBytes)!!
                load(xKey, xValue)
            }
        }

        onSaveFunc = if (onSave != null) {
            onSave
        } else {
            { key, value, save ->
                val xKey = serializer.serialize(key)!!.encodeToBase58String()
                val xValue = serializer.serialize(value)!!.encodeToBase58String()
                save(xKey, xValue)
            }
        }

        // Make sure that the timer is run on shutdown. A HARD shutdown will just POW! kill it, a "nice" shutdown will run the hook
        Runtime.getRuntime().addShutdownHook(thread)
    }

    abstract fun onSave(key: Any, value: Any?)

    abstract fun doLoad()

    abstract fun doSave()

    protected fun load() {
        // if we cannot load, then we create a properties file.
        if (!dbFile.canRead() && !dbFile.parentFile.mkdirs() && !dbFile.createNewFile()) {
            throw IOException("Cannot create file")
        }

        try {
            synchronized(dbFile) {
                lastModifiedTime = dbFile.lastModified()
                doLoad()
            }
        } catch (e: IOException) {
            logger.error("Cannot load properties!", e)
        }
    }

    private fun save(key: Any, value: Any?) {
        if (readOnly) {
            // don't accidentally save this!
            return
        }

        // if we cannot save, then we create a NEW properties file. It could have been DELETED out from under us (while in use!)
        if (!dbFile.canRead() && !dbFile.parentFile.mkdirs() && !dbFile.createNewFile()) {
            throw IOException("Cannot create file")
        }

        try {
            synchronized(dbFile) {
                if (value != null) {
                    try {
                        onSaveFunc(key, value, saveFunc)
                    } catch (e: Exception) {
                        logger.error("Unable to parse property ($dbFile) [$key] : $value", e)
                    }
                } else {
                    // value was deleted
                    saveFunc(key, null)
                }

                doSave()
                lastModifiedTime = dbFile.lastModified()
            }
            isDirty = false
        } catch (e: IOException) {
            logger.error("Properties cannot save to: $dbFile", e)
        }
    }

    private fun save() {
        if (readOnly || !isDirty) {
            // don't accidentally save this!
            return
        }

        // if we cannot save, then we create a NEW properties file. It could have been DELETED out from under us (while in use!)
        if (!dbFile.canRead() && !dbFile.parentFile.mkdirs() && !dbFile.createNewFile()) {
            throw IOException("Cannot create file")
        }

        try {
            synchronized(dbFile) {
                loadedProps.forEach { (k, v) ->
                    if (k == versionTag) {
                        save(k, v.toString())
                    } else {
                        try {
                            onSaveFunc(k, v, saveFunc)
                        } catch (e: Exception) {
                            logger.error("Unable to parse property ($dbFile) [$k] : $v", e)
                        }
                    }
                }

                doSave()
                lastModifiedTime = dbFile.lastModified()
            }
        } catch (e: IOException) {
            logger.error("Properties cannot save to: $dbFile", e)
        }
    }

    override fun setVersion(version: Long) {
        if (!readOnly) {
            // this is because `setVersion` is called internally, and we don't want to encounter errors during initialization
            set(versionTag, version)
        }
    }

    override fun file(): File {
        return dbFile
    }

    override fun size(): Int {
        return loadedProps.size
    }

    override fun contains(key: Any): Boolean {
        if (autoLoad) {
            // we want to check the last modified time when getting, because if we edit the on-disk file, we want to load those changes
            val lastModifiedTime = dbFile.lastModified()
            if (this.lastModifiedTime != lastModifiedTime) {
                // we want to reload the info
                load()
            }
        }

        return loadedProps[key] != null
    }

    override operator fun <V> get(key: Any): V? {
        if (autoLoad) {
            // we want to check the last modified time when getting, because if we edit the on-disk file, we want to load those changes
            val lastModifiedTime = dbFile.lastModified()
            if (this.lastModifiedTime != lastModifiedTime) {
                // we want to reload the info
                load()
            }
        }

        val any = loadedProps[key]
        if (any != null) {
            @Suppress("UNCHECKED_CAST")
            return any as V
        }

        return null
    }

    override operator fun set(key: Any, data: Any?) {
        if (readOnly) {
            if (readOnlyViolent) {
                throw IOException("Unable to save data in $dbFile for $key : $data")
            } else {
                return
            }
        }

        val hasChanged = if (data == null) {
            loadedProps.remove(key) != null
        } else {
            val prev = loadedProps.put(key, data)
            prev !== data
        }

        // every time we set info, we want to save it to disk (so the file on disk will ALWAYS be current, and so we can modify it as we choose)
        if (hasChanged) {
            isDirty = true
            save(key, data)
        }
    }

    /**
     * Deletes all contents of this storage, and if applicable, it's location on disk.
     */
    override fun deleteAll() {
        if (readOnly) {
            if (readOnlyViolent) {
                throw IOException("Unable to delete all data in $dbFile")
            } else {
                return
            }
        }

        loadedProps.clear()
        dbFile.delete()
    }

    /**
     * Closes this storage (and if applicable, flushes its content to disk)
     */
    override fun close() {
        Runtime.getRuntime().removeShutdownHook(thread)
        save()

        loadedProps.clear()
    }
}
