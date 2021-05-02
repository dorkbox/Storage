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

import com.esotericsoftware.kryo.io.ByteBufferOutput
import com.esotericsoftware.kryo.io.Input
import dorkbox.serializers.SerializationManager
import dorkbox.storage.Storage
import mu.KLogger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Java property files
 */
internal class PropertyStore(
    private val dbFile: File,
    private val autoLoad: Boolean,
    private val readOnly: Boolean,
    private val readOnlyViolent: Boolean,
    private val serializationManager: SerializationManager<*>,
    private val logger: KLogger
) : Storage() {

    companion object {
        private val comparator = Comparator<Any> { o1, o2 -> o1.toString().compareTo(o2.toString()) }
    }

    private val thread = Thread { close() }

    @Volatile
    private var lastModifiedTime = 0L

    private var output = ByteBufferOutput(64, 65535) // A reasonable max size of an object. We don't want to support it being TOO big
    private var input = Input()

    private val loadedProps = ConcurrentHashMap<String, Any>()

    init {
        load()

        // Make sure that the timer is run on shutdown. A HARD shutdown will just POW! kill it, a "nice" shutdown will run the hook
        Runtime.getRuntime().addShutdownHook(thread)

        logger.info("Property file storage initialized at: '$dbFile'")
    }

    private fun load() {
        // if we cannot load, then we create a properties file.
        if (!dbFile.canRead() && !dbFile.parentFile.mkdirs() && !dbFile.createNewFile()) {
            throw IOException("Cannot create file")
        }

        try {
            synchronized(dbFile) {
                FileInputStream(dbFile).use { fileStream ->
                    val properties = Properties()
                    properties.load(fileStream)
                    lastModifiedTime = dbFile.lastModified()

                    properties.entries.forEach { (k, v) ->
                        val key = k as String
                        val value = Base64.getDecoder().decode(v as String)

                        input.reset()
                        input.buffer = value

                        try {
                            val valueObject = serializationManager.readFullClassAndObject(input)
                            if (valueObject != null) {
                                loadedProps[key] = valueObject
                            } else {
                                logger.error("Unable to parse property (file: $dbFile) $key : $value")
                            }
                        } catch (e: Exception) {
                            logger.error("Unable to parse property (file: $dbFile) $key : $value", e)
                        }
                    }
                    properties.clear()
                }
            }
        } catch (e: IOException) {
            logger.error("Cannot load properties!", e)
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
                val properties = object : Properties() {
                    override fun keys(): Enumeration<Any> {
                        val keysEnum = super.keys()

                        val vector = Vector<Any>(size())
                        while (keysEnum.hasMoreElements()) {
                            vector.add(keysEnum.nextElement())
                        }

                        vector.sortWith(comparator)
                        return vector.elements()
                    }
                }

                loadedProps.forEach { (key, value) ->
                    // have to serialize the value
                    output.reset()
                    try {
                        serializationManager.writeFullClassAndObject(output, value)
                        output.byteBuffer.flip()
                        val valueEncoded = Base64.getEncoder().encode(output.byteBuffer)
                        properties[key] = String(valueEncoded.array(), 0, valueEncoded.limit())
                    } catch (e: Exception) {
                        logger.error("Unable to save property: $key $value")
                    }
                }

                FileOutputStream(dbFile, false).use { fos ->
                    properties.store(fos, "Storage data, Version: $version")
                    fos.flush()
                    properties.clear()
                    lastModifiedTime = dbFile.lastModified()
                }
            }
        } catch (e: IOException) {
            logger.error("Properties cannot save to: $dbFile", e)
        }
    }

    override fun file(): File {
        return dbFile
    }

    override fun setVersion(version: Int) {
        super.setVersion(version)
    }

    override fun size(): Int {
        return loadedProps.size
    }

    override fun contains(key: String): Boolean {
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

    override operator fun <T> get(key: String): T? {
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
            return any as T
        }

        return null
    }

    override operator fun set(key: String, data: Any?) {
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
        Runtime.getRuntime().removeShutdownHook(thread)
        save()
    }
}


