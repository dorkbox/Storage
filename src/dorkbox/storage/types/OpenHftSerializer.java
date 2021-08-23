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
//package dorkbox.storage.types;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import net.openhft.chronicle.bytes.Bytes;
//import net.openhft.chronicle.core.util.ReadResolvable;
//import net.openhft.chronicle.hash.serialization.BytesReader;
//import net.openhft.chronicle.hash.serialization.BytesWriter;
//import net.openhft.chronicle.wire.WireIn;
//import net.openhft.chronicle.wire.WireOut;
//
//@SuppressWarnings("unchecked")
//public final class OpenHftSerializer<T> implements BytesReader<T>, BytesWriter<T>, ReadResolvable<OpenHftSerializer<T>> {
//
//	private static final Logger LOGGER = LoggerFactory.getLogger(OpenHftSerializer.class);
//	// private final Serializer<T> SERIALIZER;
//
//    public OpenHftSerializer() {
//    	// this.SERIALIZER = new Serializer<T>((Class<T>) AndesUtil.getTypeArguments(OpenHftSerializer.class, this.getClass()).get(0));
//	}
//
//	@Override
//	public void write(Bytes out, T toWrite) {
//		// try {
//		// 	byte[] serizliedBytes = this.SERIALIZER.serialize(toWrite);
//		// 	out.writeInt(serizliedBytes.length);
//		// 	out.write(serizliedBytes, 0, serizliedBytes.length);
//		// } catch (Exception e) {
//		// 	LOGGER.error("Error while persisting data.", e);
//		// }
//	}
//
//	@Override
//	public T read(Bytes in, T using) {
//		// try {
//		// 	int serializedBytesLength = in.readInt();
//		// 	byte[] serizliedBytes = new byte[serializedBytesLength];
//		// 	in.read(serizliedBytes, 0, serializedBytesLength);
//		// 	using = this.SERIALIZER.deserialize(serizliedBytes);
//		// } catch (Exception e) {
//		// 	LOGGER.error("Error while reading data.", e);
//		// }
//		// return using;
//        return null;
//	}
//
//    @Override
//    public void writeMarshallable(WireOut wireOut) {
//        // no fields to write
//    }
//
//    @Override
//    public void readMarshallable(WireIn wireIn) {
//        // no fields to read
//    }
//
//	@Override
//	public OpenHftSerializer<T> readResolve() {
//		return this;
//	}
//}
