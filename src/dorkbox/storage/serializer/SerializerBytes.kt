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
package dorkbox.storage.serializer

import com.esotericsoftware.kryo.Kryo
import dorkbox.objectPool.ObjectPool
import dorkbox.objectPool.Pool
import dorkbox.objectPool.PoolObject
import org.slf4j.LoggerFactory

class SerializerBytes(onNewKryoSerializer: Kryo.() -> Unit) {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(SerializerBytes::class.java)
    }

    private val kryoPool: Pool<KryoBytes>

    init {
        val poolObject = object : PoolObject<KryoBytes>() {
            override fun newInstance(): KryoBytes {
                val kryoBytes = KryoBytes()

                // default values used, this covers the "version" tag
                kryoBytes.register(String::class.java)
                kryoBytes.register(Long::class.java)

                onNewKryoSerializer(kryoBytes)
                return kryoBytes
            }
        }

        kryoPool = ObjectPool.nonBlocking(poolObject)
    }


    @Throws(Exception::class)
    fun serialize(`object`: Any?): ByteArray {
        val kryo = kryoPool.take()
        return try {
            kryo.write(`object`).toBytes()
        } catch (e: Exception) {
            LOGGER.error("Error while serializing the object.", e)
            throw e
        } finally {
            kryoPool.put(kryo)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @Throws(Exception::class)
    fun serialize(`object`: Any?, type: Class<*>): ByteArray {
        val kryo = kryoPool.take()
        return try {
            kryo.writeObject(`object`).toBytes()
        } catch (e: Exception) {
            LOGGER.error("Error while serializing the object.", e)
            throw e
        } finally {
            kryoPool.put(kryo)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(Exception::class)
    fun <T> deserialize(bytes: ByteArray): T? {
        val kryo = kryoPool.take()
        try {
            if (bytes.isNotEmpty()) {
                return kryo.read(bytes) as T
            }
        } catch (e: Exception) {
            LOGGER.error("Error while deserializing the bytes.", e)
            throw e
        } finally {
            kryoPool.put(kryo)
        }

        return null
    }
}
