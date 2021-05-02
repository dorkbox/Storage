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
package dorkbox.storage

import dorkbox.storage.types.Storage
import dorkbox.storage.types.Storage.size
import dorkbox.storage.types.Storage.close
import dorkbox.storage.types.Storage.get
import dorkbox.storage.types.Storage.contains
import dorkbox.storage.types.Storage.delete
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class StorageTest {
    @Test
    fun testCreateDB() {
        // val store = Storage.Memory().build()
        StorageSystem.shutdown()
        StorageSystem.delete(TEST_DB)
        var storage: Storage = StorageSystem.Disk().file(TEST_DB).build()
        val numberOfRecords1 = storage.size()
        val size1: Long = storage.getFileSize()
        Assert.assertEquals("count is not correct", 0, numberOfRecords1.toLong())
        Assert.assertEquals("size is not correct", initialSize, size1)
        storage.close()
        storage = StorageSystem.Disk().file(TEST_DB).build()
        val numberOfRecords2 = storage.size()
        val size2: Long = storage.getFileSize()
        Assert.assertEquals("Record count is not the same", numberOfRecords1.toLong(), numberOfRecords2.toLong())
        Assert.assertEquals("size is not the same", size1, size2)
        StorageSystem.close(storage)
    }

    @Test
    fun testAddAsOne() {
        try {
            var storage: Storage = StorageSystem.Disk().file(TEST_DB).build()
            for (i in 0 until total) {
                add(storage, i)
            }
            StorageSystem.close(storage)
            storage = StorageSystem.Disk().file(TEST_DB).build()
            for (i in 0 until total) {
                val record1Data = createData(i)
                val readRecord = readRecord(storage, i)
                Assert.assertEquals("Object is not the same", record1Data, readRecord)
            }
            StorageSystem.close(storage)
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail("Error!")
        }
    }

    /**
     * Adds data to storage using the SAME key each time (so each entry is overwritten).
     */
    @Test
    fun testAddNoKeyRecords() {
        try {
            var storage: Storage = StorageSystem.Disk().file(TEST_DB).build()
            val storageKey = StorageKey("foobar!")
            for (i in 0 until total) {
                log("adding record $i...")
                val addRecord = createData(i)
                storage.put(storageKey, addRecord)
                log("reading record $i...")
                val readData: String = storage.get(storageKey)!!
                Assert.assertEquals("Object is not the same", addRecord, readData)
            }
            StorageSystem.close(storage)
            storage = StorageSystem.Disk().file(TEST_DB).build()
            val dataCheck = createData(total - 1)
            log("reading record " + (total - 1) + "...")
            val readData: String = storage.get(storageKey)

            // the ONLY entry in storage should be the last one that we added
            Assert.assertEquals("Object is not the same", dataCheck, readData)
            val numberOfRecords1 = storage.size()
            val size1: Long = storage.getFileSize()
            Assert.assertEquals("count is not correct", numberOfRecords1.toLong(), 1)
            Assert.assertEquals("size is not correct", size1, initialSize + sizePerRecord)
            StorageSystem.close(storage)
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail("Error!")
        }
    }

    @Test
    fun testAddRecords_DelaySaveA() {
        try {
            var storage: Storage = StorageSystem.Disk().file(TEST_DB).build()
            for (i in 0 until total) {
                add(storage, i)
            }
            synchronized(Thread.currentThread()) {
                Thread.currentThread().wait(storage.getSaveDelay() + 1000L)
            }
            for (i in 0 until total) {
                val record1Data = createData(i)
                val readRecord = readRecord(storage, i)
                Assert.assertEquals("Object is not the same", record1Data, readRecord)
            }
            StorageSystem.close(storage)
            storage = StorageSystem.Disk().file(TEST_DB).build()
            for (i in 0 until total) {
                val dataCheck = createData(i)
                val readRecord = readRecord(storage, i)
                Assert.assertEquals("Object is not the same", dataCheck, readRecord)
            }
            StorageSystem.close(storage)
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail("Error!")
        }
    }

    @Test
    fun testAddRecords_DelaySaveB() {
        try {
            var storage: Storage = StorageSystem.Disk().file(TEST_DB).build()
            for (i in 0 until total) {
                add(storage, i)
            }
            for (i in 0 until total) {
                val record1Data = createData(i)
                val readRecord = readRecord(storage, i)
                Assert.assertEquals("Object is not the same", record1Data, readRecord)
            }
            StorageSystem.close(storage)
            storage = StorageSystem.Disk().file(TEST_DB).build()
            for (i in 0 until total) {
                val dataCheck = createData(i)
                val readRecord = readRecord(storage, i)
                Assert.assertEquals("Object is not the same", dataCheck, readRecord)
            }
            StorageSystem.close(storage)
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail("Error!")
        }
    }

    @Test
    fun testLoadRecords() {
        try {
            var storage: Storage = StorageSystem.Disk().file(TEST_DB).register(Data::class.java).build()
            for (i in 0 until total) {
                val addRecord = add(storage, i)
                val readRecord = readRecord(storage, i)
                Assert.assertEquals("Object is not the same", addRecord, readRecord)
            }
            StorageSystem.close(storage)
            storage = StorageSystem.Disk().file(TEST_DB).register(Data::class.java).build()
            for (i in 0 until total) {
                val dataCheck = createData(i)
                val readRecord = readRecord(storage, i)
                Assert.assertEquals("Object is not the same", dataCheck, readRecord)
            }

            // now test loading data
            val data = Data()
            val createKey: StorageKey = createKey(63)
            makeData(data)
            storage.put(createKey, data)
            var data2: Data
            data2 = storage.get(createKey, Data())
            Assert.assertEquals("Object is not the same", data, data2)
            StorageSystem.close(storage)
            storage = StorageSystem.Disk().file(TEST_DB).register(Data::class.java).build()
            data2 = storage.get(createKey, Data())
            Assert.assertEquals("Object is not the same", data, data2)
            StorageSystem.close(storage)
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail("Error!")
        }
    }

    @Test
    @Throws(IOException::class)
    fun testAddRecordsDelete1Record() {
        if (total < 4) {
            throw IOException("Unable to run test with too few entries.")
        }
        try {
            var storage: Storage = StorageSystem.Disk().file(TEST_DB).register(Data::class.java).build()
            for (i in 0 until total) {
                val addRecord = add(storage, i)
                val readRecord = readRecord(storage, i)
                Assert.assertEquals("Object is not the same", addRecord, readRecord)
            }
            StorageSystem.close(storage)
            storage = StorageSystem.Disk().file(TEST_DB).register(Data::class.java).build()
            for (i in 0 until total) {
                val dataCheck = createData(i)
                val readRecord = readRecord(storage, i)
                Assert.assertEquals("Object is not the same", dataCheck, readRecord)
            }

            // make sure now that we can delete one of the records.
            deleteRecord(storage, 3)
            var readRecord = readRecord(storage, 9)
            var dataCheck = createData(9)
            Assert.assertEquals("Object is not the same", dataCheck, readRecord)
            if (storage.contains(createKey(3))) {
                Assert.fail("record NOT successfully deleted.")
            }

            // now we add 3 back
            val addRecord = add(storage, 3)
            dataCheck = createData(3)
            Assert.assertEquals("Object is not the same", dataCheck, addRecord)
            StorageSystem.close(storage)
            storage = StorageSystem.Disk().file(TEST_DB).register(Data::class.java).build()

            // check 9 again
            readRecord = readRecord(storage, 9)
            dataCheck = createData(9)
            Assert.assertEquals("Object is not the same", dataCheck, readRecord)

            // check 3 again
            readRecord = readRecord(storage, 3)
            dataCheck = createData(3)
            Assert.assertEquals("Object is not the same", dataCheck, readRecord)
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail("Error!")
        }
    }

    @Test
    fun testUpdateRecords() {
        try {
            var storage: Storage = StorageSystem.Disk().file(TEST_DB).register(Data::class.java).build()
            for (i in 0 until total) {
                val addRecord = add(storage, i)
                val readRecord = readRecord(storage, i)
                Assert.assertEquals("Object is not the same", addRecord, readRecord)
            }
            StorageSystem.close(storage)
            storage = StorageSystem.Disk().file(TEST_DB).register(Data::class.java).build()
            var updateRecord = updateRecord(storage, 3, createData(3) + "new")
            var readRecord = readRecord(storage, 3)
            Assert.assertEquals("Object is not the same", updateRecord, readRecord)
            StorageSystem.close(storage)
            storage = StorageSystem.Disk().file(TEST_DB).register(Data::class.java).build()
            readRecord = readRecord(storage, 3)
            Assert.assertEquals("Object is not the same", updateRecord, readRecord)
            updateRecord = updateRecord(storage, 3, createData(3))
            StorageSystem.close(storage)
            storage = StorageSystem.Disk().file(TEST_DB).register(Data::class.java).build()
            readRecord = readRecord(storage, 3)
            Assert.assertEquals("Object is not the same", updateRecord, readRecord)
            StorageSystem.close(storage)
            storage = StorageSystem.Disk().file(TEST_DB).register(Data::class.java).build()
            updateRecord = updateRecord(storage, 0, createData(0) + "new")
            readRecord = readRecord(storage, 0)
            Assert.assertEquals("Object is not the same", updateRecord, readRecord)
            StorageSystem.close(storage)
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail("Error!")
        }
    }

    @Test
    fun testSaveAllRecords() {
        try {
            var storage: Storage = StorageSystem.Disk().file(TEST_DB).register(Data::class.java).build()
            for (i in 0 until total) {
                val data = Data()
                makeData(data)
                val createKey: StorageKey = createKey(i)
                storage.put(createKey, data)
            }
            StorageSystem.close(storage)
            val data = Data()
            makeData(data)
            storage = StorageSystem.Disk().file(TEST_DB).register(Data::class.java).build()
            for (i in 0 until total) {
                val createKey: StorageKey = createKey(i)
                var data2: Data
                data2 = storage.get(createKey, Data())
                Assert.assertEquals("Object is not the same", data, data2)
            }
            StorageSystem.close(storage)
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail("Error!")
        }
    }

    class Data {
        var string: String? = null
        var strings: Array<String?>
        var ints: IntArray
        var shorts: ShortArray
        var floats: FloatArray
        var doubles: DoubleArray
        var longs: LongArray
        var bytes: ByteArray
        var chars: CharArray
        var booleans: BooleanArray
        var Ints: Array<Int>
        var Shorts: Array<Short>
        var Floats: Array<Float>
        var Doubles: Array<Double>
        var Longs: Array<Long>
        var Bytes: Array<Byte>
        var Chars: Array<Char>
        var Booleans: Array<Boolean>
        override fun hashCode(): Int {
            val prime = 31
            var result = 1
            result = prime * result + Arrays.hashCode(Booleans)
            result = prime * result + Arrays.hashCode(Bytes)
            result = prime * result + Arrays.hashCode(Chars)
            result = prime * result + Arrays.hashCode(Doubles)
            result = prime * result + Arrays.hashCode(Floats)
            result = prime * result + Arrays.hashCode(Ints)
            result = prime * result + Arrays.hashCode(Longs)
            result = prime * result + Arrays.hashCode(Shorts)
            result = prime * result + Arrays.hashCode(booleans)
            result = prime * result + Arrays.hashCode(bytes)
            result = prime * result + Arrays.hashCode(chars)
            result = prime * result + Arrays.hashCode(doubles)
            result = prime * result + Arrays.hashCode(floats)
            result = prime * result + Arrays.hashCode(ints)
            result = prime * result + Arrays.hashCode(longs)
            result = prime * result + Arrays.hashCode(shorts)
            result = prime * result + if (string == null) 0 else string.hashCode()
            result = prime * result + Arrays.hashCode(strings)
            return result
        }

        override fun equals(obj: Any?): Boolean {
            if (this === obj) {
                return true
            }
            if (obj == null) {
                return false
            }
            if (javaClass != obj.javaClass) {
                return false
            }
            val other = obj as Data
            if (!Arrays.equals(Booleans, other.Booleans)) {
                return false
            }
            if (!Arrays.equals(Bytes, other.Bytes)) {
                return false
            }
            if (!Arrays.equals(Chars, other.Chars)) {
                return false
            }
            if (!Arrays.equals(Doubles, other.Doubles)) {
                return false
            }
            if (!Arrays.equals(Floats, other.Floats)) {
                return false
            }
            if (!Arrays.equals(Ints, other.Ints)) {
                return false
            }
            if (!Arrays.equals(Longs, other.Longs)) {
                return false
            }
            if (!Arrays.equals(Shorts, other.Shorts)) {
                return false
            }
            if (!Arrays.equals(booleans, other.booleans)) {
                return false
            }
            if (!Arrays.equals(bytes, other.bytes)) {
                return false
            }
            if (!Arrays.equals(chars, other.chars)) {
                return false
            }
            if (!Arrays.equals(doubles, other.doubles)) {
                return false
            }
            if (!Arrays.equals(floats, other.floats)) {
                return false
            }
            if (!Arrays.equals(ints, other.ints)) {
                return false
            }
            if (!Arrays.equals(longs, other.longs)) {
                return false
            }
            if (!Arrays.equals(shorts, other.shorts)) {
                return false
            }
            if (string == null) {
                if (other.string != null) {
                    return false
                }
            } else if (string != other.string) {
                return false
            }
            return if (!Arrays.equals(strings, other.strings)) {
                false
            } else true
        }

        override fun toString(): String {
            return "Data"
        }
    }

    companion object {
        var total = 10

        // the initial size is specified during disk.storage construction, and is based on the number of padded records.
        private const val initialSize = 1024L

        // this is the size for each record (determined by looking at the output when writing the file)
        private const val sizePerRecord = 23
        private val TEST_DB = File("sampleFile.records")
        fun log(s: String?) {
            System.err.println(s)
        }

        private fun createData(number: Int): String {
            return "$number data for record # $number"
        }

        fun add(storage: Storage, number: Int): String {
            val record1Data = createData(number)
            val record1Key: StorageKey = createKey(number)
            log("adding record $number...")
            storage.put(record1Key, record1Data)
            return record1Data
        }

        fun readRecord(storage: Storage, number: Int): String {
            val record1Key: StorageKey = createKey(number)
            log("reading record $number...")
            val readData: String = storage.get(record1Key)!!
            log("\trecord $number data: '$readData'")
            return readData
        }

        fun deleteRecord(storage: Storage, nNumber: Int) {
            val record1Key: StorageKey = createKey(nNumber)
            log("deleting record $nNumber...")
            storage.delete(record1Key)
        }

        private fun updateRecord(storage: Storage, number: Int, newData: String): String {
            val record1Key: StorageKey = createKey(number)
            log("updating record $number...")
            storage.put(record1Key, newData)
            return newData
        }

        private fun createKey(number: Int): StorageKey {
            return StorageKey("foo$number")
        }

        // from kryo unit test.
        private fun makeData(data: Data) {
            val buffer = StringBuilder(128)
            for (i in 0..2) {
                buffer.append('a')
            }
            data.string = buffer.toString()
            data.strings = arrayOf("ab012", "", null, "!@#$", "�����")
            data.ints = intArrayOf(-1234567, 1234567, -1, 0, 1, Int.MAX_VALUE, Int.MIN_VALUE)
            data.shorts = shortArrayOf(
                (-12345).toShort(),
                12345.toShort(),
                (-1).toShort(),
                0.toShort(),
                1.toShort(),
                Short.MAX_VALUE,
                Short.MIN_VALUE
            )
            data.floats =
                floatArrayOf(0f, -0f, 1f, -1f, 123456f, -123456f, 0.1f, 0.2f, -0.3f, Math.PI.toFloat(), Float.MAX_VALUE, Float.MIN_VALUE)
            data.doubles =
                doubleArrayOf(0.0, -0.0, 1.0, -1.0, 123456.0, -123456.0, 0.1, 0.2, -0.3, Math.PI, Double.MAX_VALUE, Double.MIN_VALUE)
            data.longs = longArrayOf(0, -0, 1, -1, 123456, -123456, 99999999999L, -99999999999L, Long.MAX_VALUE, Long.MIN_VALUE)
            data.bytes = byteArrayOf((-123).toByte(), 123.toByte(), (-1).toByte(), 0.toByte(), 1.toByte(), Byte.MAX_VALUE, Byte.MIN_VALUE)
            data.chars =
                charArrayOf(32345.toChar(), 12345.toChar(), 0.toChar(), 1.toChar(), 63.toChar(), Character.MAX_VALUE, Character.MIN_VALUE)
            data.booleans = booleanArrayOf(true, false)
            data.Ints = arrayOf(-1234567, 1234567, -1, 0, 1, Int.MAX_VALUE, Int.MIN_VALUE)
            data.Shorts = arrayOf(-12345, 12345, -1, 0, 1, Short.MAX_VALUE, Short.MIN_VALUE)
            data.Floats = arrayOf(
                0.0f,
                -0.0f,
                1.0f,
                -1.0f,
                123456.0f,
                -123456.0f,
                0.1f,
                0.2f,
                -0.3f,
                Math.PI.toFloat(),
                Float.MAX_VALUE,
                Float.MIN_VALUE
            )
            data.Doubles = arrayOf(0.0, -0.0, 1.0, -1.0, 123456.0, -123456.0, 0.1, 0.2, -0.3, Math.PI, Double.MAX_VALUE, Double.MIN_VALUE)
            data.Longs = arrayOf(0L, -0L, 1L, -1L, 123456L, -123456L, 99999999999L, -99999999999L, Long.MAX_VALUE, Long.MIN_VALUE)
            data.Bytes = arrayOf(-123, 123, -1, 0, 1, Byte.MAX_VALUE, Byte.MIN_VALUE)
            data.Chars = arrayOf(32345, 12345, 0, 1, 63, Character.MAX_VALUE, Character.MIN_VALUE)
            data.Booleans = arrayOf(true, false)
        }
    }
}
