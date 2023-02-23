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
import dorkbox.storage.AccessFunc
import mu.KLogger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

/**
 * Java property file storage system
 */
class PropertyStore(
    dbFile: File,
    autoLoad: Boolean,
    readOnly: Boolean,
    readOnlyViolent: Boolean,
    logger: KLogger,
    onNewKryo: Kryo.() -> Unit,
    onLoad: AccessFunc?,
    onSave: AccessFunc?,
) : StringStore(dbFile, autoLoad, readOnly, readOnlyViolent, logger, onNewKryo, onLoad, onSave) {

    private val propertiesOnDisk = object : Properties() {
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

    init {
        load()
        init("Property file storage initialized at: '$dbFile'")
    }

    override fun onSave(key: Any, value: Any?) {
        propertiesOnDisk[key] = value
    }

    override fun doLoad() {
        FileInputStream(dbFile).use { fileStream ->
            propertiesOnDisk.load(fileStream)

            propertiesOnDisk.entries.forEach { (k, v) ->
                if (k == versionTag) {
                    loadFunc(k, (v as String).toLong())
                    propertiesOnDisk[k] = v
                } else {
                    try {
                        propertiesOnDisk[k] = v
                        onLoadFunc(k, v, loadFunc)
                    } catch (e: Exception) {
                        logger.error("Unable to parse property ($dbFile) [$k] : $v", e)
                    }
                }
            }
        }
    }

    override fun doSave() {
        FileOutputStream(dbFile, false).use { fos ->
            propertiesOnDisk.store(fos, "Storage Version: ${getVersion()}")
            fos.flush()
        }
    }

    /**
     * Deletes all contents of this storage, and if applicable, it's location on disk.
     */
    override fun deleteAll() {
        super.deleteAll()

        propertiesOnDisk.clear()
    }

    /**
     * Closes this storage (and if applicable, flushes its content to disk)
     */
    override fun close() {
        super.close()

        propertiesOnDisk.clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PropertyStore) return false
        if (!super.equals(other)) return false

        if (propertiesOnDisk != other.propertiesOnDisk) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + propertiesOnDisk.hashCode()
        return result
    }

    override fun toString(): String {
        return "PropertyStore(file=$propertiesOnDisk)"
    }
}
