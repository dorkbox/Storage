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

/**
 * Lightning Memory Database
 *
 * https://github.com/lmdbjava/lmdbjava
 */
class LmdbStore(
    val dbFile: File,
    val readOnly: Boolean,
    logger: Logger
) : Storage(logger) {

//    private val env: Env<ByteArray>
//    private val db: Dbi<ByteArray>

    init {
//        val prep = Env.create(ByteArrayProxy.PROXY_BA).setMapSize(1048760).setMaxDbs(1)
//
//        env = if (dbFile.isDirectory) {
//            prep.open(dbFile)
//        } else {
//            // The database lock file is the path with "-lock" appended.
//            prep.open(dbFile, EnvFlags.MDB_NOSUBDIR)
//        }
//
//        db = env.openDbi("machine-keys", DbiFlags.MDB_CREATE)

        init("LMDB storage initialized at: '$dbFile'")
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
//        val keyBytes = getBytes(key)
//
//        return env.txnRead().use { txn ->
//            db.get(txn, keyBytes) ?: return null
//        }
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
//            env.txnWrite().use { txn ->
//                db.delete(txn, keyBytes)
//
//                // An explicit commit is required, otherwise Txn.close() rolls it back.
//                txn.commit()
//            }
//        }
//        else {
//            env.txnWrite().use { txn ->
//                try {
//                    db.put(txn, keyBytes, bytes)
//                } catch (e: Exception) {
//                    logger.error("Unable to save to LMDB!", e)
//                }
//
//                // An explicit commit is required, otherwise Txn.close() rolls it back.
//                txn.commit()
//            }
//        }
//    }

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
        TODO("Not yet implemented")
    }

    override fun contains(key: Any): Boolean {
        TODO("Not yet implemented")
    }

    override fun <V> get(key: Any): V? {
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
//        db.close()
//        env.close()
    }
}

//
///**
// * Lightning Memory Database
// *
// * https://github.com/lmdbjava/lmdbjava
// */
//class LmdbStore(val dbFile: File, val logger: KLogger): GenericStore {
//    companion object {
//        fun type(dbFile: String) : StorageType {
//            return type(File(dbFile))
//        }
//
//        fun type(dbFile: File) = object : StorageType {
//            override fun create(logger: KLogger): SettingsStore {
//                return SettingsStore(logger, LmdbStore(dbFile.absoluteFile, logger))
//            }
//        }
//    }
//
//    private val env: Env<ByteArray>
//    private val db: Dbi<ByteArray>
//
//    init {
//        val prep = Env.create(ByteArrayProxy.PROXY_BA).setMapSize(1048760).setMaxDbs(1)
//
//        env = if (dbFile.isDirectory) {
//            prep.open(dbFile)
//        }
//        else {
//            // The database lock file is the path with "-lock" appended.
//            prep.open(dbFile, EnvFlags.MDB_NOSUBDIR)
//        }
//
//        db = env.openDbi("machine-keys", DbiFlags.MDB_CREATE)
//
//        logger.info("LMDB storage initialized at: '$dbFile'")
//    }
//
//    // byte 0 is SALT
//    private val saltBuffer = ByteArray(1) {0}
//
//    // byte 1 is private key
//    private val privateKeyBuffer = ByteArray(1) {1}
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
//        val keyBytes = getBytes(key)
//
//        return env.txnRead().use { txn ->
//            db.get(txn, keyBytes) ?: return null
//        }
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
//            env.txnWrite().use { txn ->
//                db.delete(txn, keyBytes)
//
//                // An explicit commit is required, otherwise Txn.close() rolls it back.
//                txn.commit()
//            }
//        }
//        else {
//            env.txnWrite().use { txn ->
//                try {
//                    db.put(txn, keyBytes, bytes)
//                } catch (e: Exception) {
//                    logger.error("Unable to save to LMDB!", e)
//                }
//
//                // An explicit commit is required, otherwise Txn.close() rolls it back.
//                txn.commit()
//            }
//        }
//    }
//
//    override fun close() {
//        db.close()
//        env.close()
//    }
//}
