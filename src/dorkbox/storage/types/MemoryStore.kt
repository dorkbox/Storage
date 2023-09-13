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

import dorkbox.storage.Storage
import org.slf4j.Logger
import java.io.File
import java.util.concurrent.*

/**
 * In-Memory storage
 */
class MemoryStore(logger: Logger) : Storage(logger) {
    private val map = ConcurrentHashMap<Any, Any>()

    init {
        init("Memory storage initialized")
    }

    override fun setVersion(version: Long) {
        // this is because `setVersion` is called internally, and we don't want to encounter errors during initialization
        set(versionTag, version)
    }

    override fun file(): File? {
        return null
    }

    override fun size(): Int {
        return map.size
    }

    override fun contains(key: Any): Boolean {
        return map.contains(key)
    }

    override operator fun <V> get(key: Any): V? {
        @Suppress("UNCHECKED_CAST")
        return map[key] as V
    }

    override operator fun set(key: Any, data: Any?) {
        if (data == null) {
            map.remove(key)
        } else {
            map[key] = data
        }
    }

    /**
     * Deletes all contents of this storage
     */
    override fun deleteAll() {
        map.clear()
    }

    override fun close() {
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemoryStore) return false

        if (map != other.map) return false

        return true
    }

    override fun hashCode(): Int {
        return map.hashCode()
    }

    override fun toString(): String {
        return "MemoryStore(map=${map.hashCode()})"
    }
}
