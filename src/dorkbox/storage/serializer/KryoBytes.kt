/*
 * Copyright 2020 dorkbox, llc
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
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

/**
 * READ and WRITE are exclusive to each other and can be performed in different threads.
 */
class KryoBytes: Kryo() {
    // for kryo serialization
    private val readerBuffer = Input()
    private val writerBuffer = Output(64, 65534)  // reasonable max size for an object

    /**
     * OUTPUT:
     * ++++++++++++++++++++++++++
     * + class and object bytes +
     * ++++++++++++++++++++++++++
     */
    @Throws(Exception::class)
    fun write(message: Any?): Output {
        writerBuffer.reset()
        writeClassAndObject(writerBuffer, message)
        return writerBuffer
    }

    /**
     * OUTPUT:
     * ++++++++++++++++++++++++++
     * + object bytes +
     * ++++++++++++++++++++++++++
     */
    @Throws(Exception::class)
    fun writeObject(message: Any?): Output {
        writerBuffer.reset()
        writeObject(writerBuffer, message)
        return writerBuffer
    }

    /**
     * NOTE: THIS CANNOT BE USED FOR ANYTHING RELATED TO RMI!
     *
     * INPUT:
     * ++++++++++++++++++++++++++
     * + class and object bytes +
     * ++++++++++++++++++++++++++
     */
    @Throws(Exception::class)
    fun read(buffer: ByteArray): Any? {
        // this properly sets the buffer info
        readerBuffer.buffer = buffer
        return readClassAndObject(readerBuffer)
    }

    /**
     * INPUT:
     * ++++++++++++++++++++++++++
     * + class and object bytes +
     * ++++++++++++++++++++++++++
     */
    @Throws(Exception::class)
    fun read(buffer: ByteArray, offset: Int, length: Int): Any? {
        // this properly sets the buffer info
        readerBuffer.setBuffer(buffer, offset, length)
        return readClassAndObject(readerBuffer)
    }

    ////////////////
    ////////////////
    ////////////////
    // for more complicated writes, sadly, we have to deal DIRECTLY with byte arrays
    ////////////////
    ////////////////
    ////////////////

    /**
     * OUTPUT:
     * ++++++++++++++++++++++++++
     * + class and object bytes +
     * ++++++++++++++++++++++++++
     */
    fun write(writer: Output, message: Any?) {
        // write the object to the NORMAL output buffer!
        writer.reset()
        writeClassAndObject(writer, message)
    }

    /**
     * INPUT:
     * ++++++++++++++++++++++++++
     * + class and object bytes +
     * ++++++++++++++++++++++++++
     */
    fun read(reader: Input): Any? {
        return readClassAndObject(reader)
    }
}
