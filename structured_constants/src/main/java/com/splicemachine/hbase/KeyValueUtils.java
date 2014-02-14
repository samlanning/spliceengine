package com.splicemachine.hbase;

import org.apache.hadoop.hbase.KeyValue;

/**
 * @author Scott Fines
 *         Date: 2/14/14
 */
public class KeyValueUtils {

		private KeyValueUtils(){}

		public static boolean matchingValue(KeyValue keyValue, byte[] value) {
				return ByteBufferArrayUtils.matchingValue(keyValue, value);
		}

		public static boolean matchingFamilyKeyValue(KeyValue keyValue, KeyValue other) {
				return ByteBufferArrayUtils.matchingFamilyKeyValue(keyValue, other);
		}
		public static boolean matchingQualifierKeyValue(KeyValue keyValue, KeyValue other) {
				return ByteBufferArrayUtils.matchingQualifierKeyValue(keyValue, other);
		}
		public static boolean matchingRowKeyValue(KeyValue keyValue, KeyValue other) {
				return ByteBufferArrayUtils.matchingRowKeyValue(keyValue, other);
		}

		public static KeyValue newKeyValue(KeyValue keyValue, byte[] value) {
				return new KeyValue(keyValue.getBuffer(),keyValue.getRowOffset(),keyValue.getRowLength(),keyValue.getBuffer(),keyValue.getFamilyOffset(),keyValue.getFamilyLength(),keyValue.getBuffer(),keyValue.getQualifierOffset(),keyValue.getQualifierLength(),keyValue.getTimestamp(), KeyValue.Type.Put,value,0,value==null ? 0 : value.length);
		}

		public static KeyValue newKeyValue(byte[] rowKey, byte[] family, byte[] qualifier, Long timestamp, byte[] value) {
				return new KeyValue(rowKey, family, qualifier, timestamp, value);
		}
}
