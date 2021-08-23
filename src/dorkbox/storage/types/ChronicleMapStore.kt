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

import com.esotericsoftware.kryo.io.ByteBufferInput
import com.esotericsoftware.kryo.io.ByteBufferOutput
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import dorkbox.storage.Storage
import mu.KLogger
import java.io.File

/**
 * Chronicle Map is a super-fast, in-memory, non-blocking, key-value store
 *
 * https://github.com/OpenHFT/Chronicle-Map
 */
class ChronicleMapStore(
    val dbFile: File,
    val readOnly: Boolean,
    logger: KLogger
) : Storage(logger) {

//    private val map = ChronicleMap.of(String::class.java, ByteArray::class.java)
//        .name("machine-keys")
////        .objectSerializer(KryoObjectSerializer())
//        .entries(1_000_000)
//        .constantValueSizeBySample(ByteArray(32))
//        .averageKeySize(16.0)
//        .createPersistedTo(dbFile)


    private var output: Output =
        ByteBufferOutput(64, 65535) // A reasonable max size of an object. We don't want to support it being TOO big
    private var input: Input = ByteBufferInput(0)

//    // byte 0 is SALT
//    private val saltBuffer = ByteArray(1) {0}
//
//    // byte 1 is private key
//    private val privateKeyBuffer = ByteArray(1) {1}

    init {
        init("ChronicleMap storage initialized at: '$dbFile'")
    }

//    private fun getBytes(key: Any): ByteArray {
//        return when (key) {
//            is InetAddress -> key.address
//            SettingsStore.saltKey -> saltBuffer
//            SettingsStore.privateKey -> privateKeyBuffer
//            else -> throw IllegalArgumentException("Unable to manage property: $key")
//        }
//    }
//
//    override fun get(key: Any): ByteArray? {
//        return map[getBytes(key)]
//    }
//
//    /**
//     * Setting to NULL removes it
//     */
//    @Suppress("DuplicatedCode")
//    override fun set(key: Any, bytes: ByteArray?) {
//        val keyBytes = getBytes(key)
//
//        if (bytes == null) {
//            map.remove(keyBytes)
//        }
//        else {
//            map[keyBytes] = bytes
//        }
//    }

    override fun setVersion(version: Long) {
        if (!readOnly) {
            // this is because `setVersion` is called internally, and we don't want to encounter errors during initialization
            set(versionTag, version)
        }
    }

    override fun file(): File? {
        return dbFile
    }

    override fun size(): Int {
        TODO("Not yet implemented")
    }

    override fun contains(key: Any): Boolean {
        TODO("Not yet implemented")
    }

    override fun <V> get(key: Any): V? {
//        input.reset()
//        input.buffer = value
//
//        val valueObject = serializationManager.readFullClassAndObject(input)

//        return map[key]
        TODO("Not yet implemented")
    }

    override fun set(key: Any, data: Any?) {
        TODO("Not yet implemented")
    }

    /**
     * Deletes all contents of this storage, and if applicable, it's location on disk.
     */
    override fun deleteAll() {

    }

    override fun close() {
//        map.close()
    }
}


////
///**
// * Chronicle Map is a super-fast, in-memory, non-blocking, key-value store
// *
// * https://github.com/OpenHFT/Chronicle-Map
// */
//class ChronicleMapStore(val dbFile: File, val logger: KLogger): GenericStore {
//    companion object {
//        fun type(dbFile: String) : StorageType {
//            return type(File(dbFile))
//        }
//
//        fun type(dbFile: File) = object : StorageType {
//            override fun create(logger: KLogger): SettingsStore {
//                return SettingsStore(logger, ChronicleMapStore(dbFile.absoluteFile, logger))
//            }
//        }
//    }
//    private val map = ChronicleMapBuilder.of(ByteArray::class.java, ByteArray::class.java)
//        .name("machine-keys")
//        .entries(1_000_000)
//        .constantValueSizeBySample(ByteArray(32))
//        .averageKeySize(16.0)
//        .createPersistedTo(dbFile)
//
//    // byte 0 is SALT
//    private val saltBuffer = ByteArray(1) {0}
//
//    // byte 1 is private key
//    private val privateKeyBuffer = ByteArray(1) {1}
//
//    init {
//        logger.info("ChronicleMap storage initialized at: '$dbFile'")
//    }
//
//    private fun getBytes(key: Any): ByteArray {
//        return when (key) {
//            is InetAddress -> key.address
//            SettingsStore.saltKey -> saltBuffer
//            SettingsStore.privateKey -> privateKeyBuffer
//            else -> throw IllegalArgumentException("Unable to manage property: $key")
//        }
//    }
//
//    override fun get(key: Any): ByteArray? {
//        return map[getBytes(key)]
//    }
//
//    /**
//     * Setting to NULL removes it
//     */
//    @Suppress("DuplicatedCode")
//    override fun set(key: Any, bytes: ByteArray?) {
//        val keyBytes = getBytes(key)
//
//        if (bytes == null) {
//            map.remove(keyBytes)
//        }
//        else {
//            map[keyBytes] = bytes
//        }
//    }
//
//    override fun close() {
//        map.close()
//    }
//}
