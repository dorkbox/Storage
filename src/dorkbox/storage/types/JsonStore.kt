/*
 * Copyright 2023 dorkbox, llc
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

import dorkbox.json.JsonException
import dorkbox.json.OutputType
import dorkbox.storage.AccessFunc
import dorkbox.storage.Storage
import mu.KLogger
import java.io.File
import java.io.IOException
import java.util.concurrent.*

/**
 * JSON property file storage system
 */
class JsonStore(
    val dbFile: File,
    val autoLoad: Boolean,
    val readOnly: Boolean,
    val readOnlyViolent: Boolean,
    logger: KLogger,
    onLoad: AccessFunc,
    onSave: AccessFunc,
) : Storage(logger) {

    companion object {
        private val comparator = Comparator<Any> { o1, o2 -> o1.toString().compareTo(o2.toString()) }
    }

    private var version: Long = 0
    private val thread = Thread { close() }

    private val json = dorkbox.json.Json()

    @Volatile
    private var lastModifiedTime = 0L

    private val loadedProps = ConcurrentHashMap<Any, Any?>()

    init {
        json.outputType = OutputType.json

        load()
        val versionInfo = loadedProps[versionTag]
        if (versionInfo !is Number) {
            loadedProps[versionTag] = version
        } else {
            setVersion(versionInfo as Long)
        }


        // Make sure that the timer is run on shutdown. A HARD shutdown will just POW! kill it, a "nice" shutdown will run the hook
        Runtime.getRuntime().addShutdownHook(thread)

        init("Property file storage initialized at: '$dbFile'")
    }

    private fun load() {
        // if we cannot load, then we create a properties file.
        if (!dbFile.canRead() && !dbFile.parentFile.mkdirs() && !dbFile.createNewFile()) {
            throw IOException("Cannot create file")
        }

        try {
            synchronized(dbFile) {
                @Suppress("UNCHECKED_CAST")
                val map = json.fromJson(HashMap::class.java, dbFile) as Map<Any, Any?>?
                map?.forEach { (k,v) ->
                    loadedProps[k] = v
                }
                lastModifiedTime = dbFile.lastModified()
            }
        } catch (e: JsonException) {
            logger.error("Cannot load JSON file!", e)
        } catch (e: IOException) {
            logger.error("Cannot load JSON file!", e)
        }
    }

    private fun save() {
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
                val data = json.toJson(loadedProps)
                val pretty = json.prettyPrint(data)
                dbFile.writeText(pretty)
                lastModifiedTime = dbFile.lastModified()
            }
        } catch (e: JsonException) {
            logger.error("JSON cannot save to: $dbFile", e)
        } catch (e: IOException) {
            logger.error("JSON cannot save to: $dbFile", e)
        }
    }

    override fun setVersion(version: Long) {
        this.version = version
    }

    override fun file(): File {
        return dbFile
    }

    override fun size(): Int {
        return loadedProps.size
    }

    override fun contains(key: Any): Boolean {
        require(key is String) {
            "Keys for a json file must be a String"
        }

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
        require(key is String) {
            "Keys for a json file must be a String"
        }

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
        require(key is String) {
            "Keys for a json file must be a String"
        }

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
            save()
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
     * Closes this storage (and if applicable, flushes it's content to disk)
     */
    override fun close() {
        if (Thread.currentThread() != thread) {
            try {
                Runtime.getRuntime().removeShutdownHook(thread)
            }
            catch (ignored: Exception) { }
            catch (ignored: RuntimeException) { }
        }
        save()
    }
}


