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
package dorkbox.storage

import dorkbox.bytes.decodeBase58
import dorkbox.bytes.encodeToBase58String
import dorkbox.storage.serializer.SerializerBytes
import dorkbox.storage.types.MemoryStore
import dorkbox.storage.types.PropertyStore
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.helpers.NOPLogger
import java.io.File


typealias AccessFunc = ((serializer: SerializerBytes, key: Any, value: Any?, load: (key: Any, value: Any?) -> Unit) -> Unit)

abstract class Storage(val logger: Logger) : AutoCloseable {
    companion object {
        /**
         * Gets the version number.
         */
        const val version = "1.11"

        init {
            // Add this project to the updates system, which verifies this class + UUID + version information
            dorkbox.updates.Updates.add(Storage::class.java, "c1281a7c576c4a9db0823763ff2fe811", version)
        }

        const val versionTag = "__VERSION__"

        private val defaultLogger: Logger = NOPLogger.NOP_LOGGER
    }

    fun init(initMessage: String) {
        // always initialize the version tag information
        if (getVersion() == -1L) {
            setVersion(0L)
        }

        LoggerFactory.getLogger("Storage").info(initMessage)
    }

    /**
     * @return the version of data stored in the database
     */
    fun getVersion(): Long {
        return get(versionTag) ?: -1L
    }

    /**
     * Sets the version of data stored in the database
     */
    abstract fun setVersion(version: Long)

    /**
     * @return the file that backs this storage
     */
    abstract fun file(): File?

    /**
     * Returns the number of objects in the database.
     */
    abstract fun size(): Int

    /**
     * Checks if there is a object corresponding to the given key.
     */
    abstract operator fun contains(key: Any): Boolean

    /**
     * Reads an object using the specific key, and casts it to the expected class
     */
    abstract operator fun <V> get(key: Any): V?

    /**
     * Returns the saved data for the specified key.
     *
     * @param key The key used to check if data already exists.
     * @param data This is the default value, and if there is no value with the key in the DB this default value will be returned AND saved.
     */
    operator fun <V> get(key: Any, data: V): V {
        val current = get(key) as Any?
        if (current == null) {
            set(key, data)
            return data
        }

        @Suppress("UNCHECKED_CAST")
        return current as V
    }

    /**
     * Saves the given data to storage with the associated key.
     *
     * Setting to NULL removes the value
     */
    abstract operator fun set(key: Any, data: Any?)

    /**
     * Deletes an object from storage.
     */
    fun delete(key: Any) {
        set(key, null)
    }

    /**
     * Deletes all contents of this storage, and if applicable, it's location on disk.
     */
    abstract fun deleteAll()

    /**
     * Closes this storage system
     */
    abstract override fun close()

    interface Builder {
        /**
         * true if the backing storage type uses strings for everything, or false if it uses something else
         */
        val isStringBased: Boolean

        /**
         * Builds the storage using the specified configuration
         */
        fun build(): Storage

        /**
         * Allows for this storage type to be shared among [build] invocations. The first [build] will create the object used by all other [build] invocations (instead of creating a new one each time)
         */
        fun shared(): Builder

        /**
         * Allows for custom load actions when keys/values need to be transformed before loading them from storage
         *
         * @param onLoad action to load data when the key/value pairs are read from storage
         */
        fun onLoad(onLoad: AccessFunc): Builder

        /**
         * Allows for custom save actions when key/values need to be translated before saving them to storage\
         *
         * @param onSave action to save data when the key/value pairs are saved to storage.
         */
        fun onSave(onSave: AccessFunc): Builder

        /**
         * Specify how to configure the kryo instance for serialization. This instance of kryo will be used to serialize the key/value data, which is subsequently saved
         */
        fun serializer(serializer: SerializerBytes): Builder

        /**
         * Assigns a logger to use for the storage system. The default is a No Operation (NOP) logger which will ignore everything.
         */
        fun logger(logger: Logger): Builder
    }


     @Suppress("UNCHECKED_CAST")
     abstract class FileBuilder<B : Builder>: Builder {
         companion object {
             private val defaultSerializer = SerializerBytes {}
         }
         abstract var onLoad: AccessFunc
         abstract var onSave: AccessFunc

         var serializer: SerializerBytes = defaultSerializer

         var shared = false

         @Volatile
         var sharedBuild: Storage? = null

         var logger: Logger = defaultLogger

         var file = File("storage.db")
         var readOnly = false
         var readOnlyViolent = false

         override val isStringBased = false

         internal fun manageShared(buildFun: () -> Storage): Storage {
             return if (shared) {
                 if (sharedBuild == null) {
                     sharedBuild = buildFun()
                 }

                 sharedBuild!!
             } else {
                 buildFun()
             }
         }

         override fun shared(): B {
             shared = true
             return this as B
         }

         override fun onLoad(onLoad: AccessFunc): Builder {
             this.onLoad = onLoad
             return this
         }

         override fun onSave(onSave: AccessFunc): Builder {
             this.onSave = onSave
             return this
         }

         override fun serializer(serializer: SerializerBytes): Builder {
             this.serializer = serializer
             return this
         }

         override fun logger(logger: Logger): B {
             this.logger = logger
             return this as B
         }

         /**
          * Specify the file to write to on disk when saving objects
          */
         fun file(file: File): B {
             this.file = file
             return this as B
         }

         /**
          * Specify the file to write to on disk when saving objects
          */
         fun file(file: String): B {
             return file(File(file))
         }

         /**
          * Mark this storage system as read only (silently drop `set` operations)
          */
         fun readOnly(): B{
             readOnly = true
             readOnlyViolent = false
             return this as B
         }

         /**
          * Mark this storage system as read only (throw an IOException for `set` operations)
          */
         fun readOnlyThrowExceptions(): B {
             readOnly = true
             readOnlyViolent = true
             return this as B
         }

         override fun equals(other: Any?): Boolean {
             if (this === other) return true
             if (other !is FileBuilder<*>) return false

             if (onLoad != other.onLoad) return false //lambda
             if (onSave != other.onSave) return false //lambda
             if (serializer != other.serializer) return false //lambda
             if (shared != other.shared) return false
             if (sharedBuild != other.sharedBuild) return false
             if (logger != other.logger) return false
             if (file != other.file) return false
             if (readOnly != other.readOnly) return false
             if (readOnlyViolent != other.readOnlyViolent) return false
             if (isStringBased != other.isStringBased) return false

             return true
         }

         override fun hashCode(): Int {
             var result = onLoad.hashCode()
             result = 31 * result + onSave.hashCode()
             result = 31 * result + serializer.hashCode()
             result = 31 * result + shared.hashCode()
             result = 31 * result + (sharedBuild?.hashCode() ?: 0)
             result = 31 * result + logger.hashCode()
             result = 31 * result + file.hashCode()
             result = 31 * result + readOnly.hashCode()
             result = 31 * result + readOnlyViolent.hashCode()
             result = 31 * result + isStringBased.hashCode()
             return result
         }
     }




    /**
     * An in-memory only storage system.
     *
     * This storage system DOES NOT care about serializing data, so `register` has no effect.
     */
    class Memory : Builder {
        private var logger: Logger = defaultLogger

        private var shared = false
        @Volatile private var sharedBuild: Storage? = null

        override val isStringBased = true

        override fun build(): Storage {
            return if (shared) {
                if (sharedBuild == null) {
                    sharedBuild = MemoryStore(logger)
                }

                sharedBuild!!
            } else {
                MemoryStore(logger)
            }
        }

        override fun shared(): Memory {
            shared = true
            return this
        }

        override fun onLoad(onLoad: AccessFunc): Builder {
            return this
        }

        override fun onSave(onSave: AccessFunc): Builder {
            return this
        }

        override fun serializer(serializer: SerializerBytes): Builder {
            return this
        }


        /**
         * Assigns a logger to use for the storage system. If null, then only errors will be logged to the error console.
         */
        override fun logger(logger: Logger): Builder {
            this.logger = logger
            return this
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Memory) return false

            if (logger != other.logger) return false
            if (shared != other.shared) return false
            if (sharedBuild != other.sharedBuild) return false
            if (isStringBased != other.isStringBased) return false

            return true
        }

        override fun hashCode(): Int {
            var result = logger.hashCode()
            result = 31 * result + shared.hashCode()
            result = 31 * result + (sharedBuild?.hashCode() ?: 0)
            result = 31 * result + isStringBased.hashCode()
            return result
        }
    }

    /**
     * Java property file storage system
     */
    class Property : FileBuilder<Property>() {
        companion object {
            // property files MUST load via strings.
            val defaultOnLoad: AccessFunc = { serializer, key, value, load ->
                val keyBytes = (key as String).decodeBase58()
                val valueBytes = (value as String).decodeBase58()

                val xKey = serializer.deserialize<Any>(keyBytes)!!
                val xValue = serializer.deserialize<Any>(valueBytes)
                load(xKey, xValue)
            }

            val defaultOnSave: AccessFunc = { serializer, key, value, save ->
                val xKey = serializer.serialize(key).encodeToBase58String()
                val xValue = serializer.serialize(value).encodeToBase58String()
                save(xKey, xValue)
            }
        }

        private var autoLoad = false

        override var onLoad: AccessFunc = defaultOnLoad
        override var onSave: AccessFunc = defaultOnSave

        override val isStringBased = true

        /**
         * Builds the storage system
         */
        override fun build(): Storage {
            return manageShared {
                PropertyStore(
                    dbFile = file.absoluteFile,
                    autoLoad = autoLoad,
                    readOnly = readOnly,
                    readOnlyViolent = readOnlyViolent,
                    logger = logger,
                    serializer = serializer,
                    onLoad = onLoad,
                    onSave = onSave
                )
            }
        }

        /**
         * Allow auto-loading any changes made to disk
         */
        fun autoLoadChanges(): Property {
            autoLoad = true
            return this
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Property) return false
            if (!super.equals(other)) return false

            if (autoLoad != other.autoLoad) return false
            if (isStringBased != other.isStringBased) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + autoLoad.hashCode()
            result = 31 * result + isStringBased.hashCode()
            return result
        }
    }

//    /**
//     * JSON file storage system
//     */
//    class Json : Storage.FileBuilder<Json>() {
//        companion object {
//            // property files MUST load via strings.
//            val defaultOnLoad: AccessFunc = { serializer, key, value, load ->
//                val keyBytes = (key as String).decodeBase58()
//                val valueBytes = (value as String).decodeBase58()
//
//                val xKey = serializer.deserialize<Any>(keyBytes)!!
//                val xValue = serializer.deserialize<Any>(valueBytes)
//                load(xKey, xValue)
//            }
//
//            val defaultOnSave: AccessFunc = { serializer, key, value, save ->
//                val xKey = serializer.serialize(key).encodeToBase58String()
//                val xValue = serializer.serialize(value).encodeToBase58String()
//                save(xKey, xValue)
//            }
//        }
//
//        private var autoLoad = false
//
//        override var onLoad: AccessFunc = defaultOnLoad
//        override var onSave: AccessFunc = defaultOnSave
//
//        override fun build(): Storage {
//            return manageShared {
//                JsonStore(file.absoluteFile, autoLoad, readOnly, readOnlyViolent, logger, onLoad, onSave)
//            }
//        }
//
//        /**
//         * Allow auto-loading any changes made to disk
//         */
//        fun autoLoadChanges(): Json {
//            autoLoad = true
//            return this
//        }
//    }

//    /**
//     * Lightning Memory Database
//     *
//     * https://github.com/lmdbjava/lmdbjava
//     */
//    class Lmdb<K> : SimpleBuilder<Lmdb<K>, K>() {
//        private var file = File("storage.db")
//        private var readOnly = false
//
//        /**
//         * Builds the storage system
//         */
//        override fun build(): Storage<K> {
//            return manageShared { LmdbStore(file.absoluteFile, readOnly, serializationManager, logger,
    //                    onLoadKey, onSaveKey, onLoadValue, onSaveValue) }
//        }
//
//        /**
//         * Specify the file to write to on disk when saving objects
//         */
//        fun file(file: File): Lmdb<K> {
//            this.file = file
//            return this
//        }
//
//        /**
//         * Specify the file to write to on disk when saving objects
//         */
//        fun file(file: String): Lmdb<K> {
//            return file(File(file))
//        }
//
//        /**
//         * Mark this storage system as read only
//         */
//        fun readOnly(): Lmdb<K> {
//            readOnly = true
//            return this
//        }
//    }
}
