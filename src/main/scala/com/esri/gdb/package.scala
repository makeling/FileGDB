package com.esri

import java.nio.ByteBuffer

import org.apache.spark.sql.{DataFrame, DataFrameReader, SQLContext}

package object gdb {

  implicit class SQLContextImplicits(sqlContext: SQLContext) extends Serializable {
    def gdb(path: String, name: String, numPartitions: Int = 8, wkid: Int = 4326): DataFrame = {
      val relation = GDBRelation(path, name, numPartitions, wkid)(sqlContext)
      sqlContext.baseRelationToDataFrame(relation)
    }
  }

  implicit class DataFrameReaderImplicits(reader: DataFrameReader) {
    def gdb(path: String, name: String, numPartitions: Int = 8, wkid: Int = 4326) = reader
      .format("com.esri.gdb")
      .option(GDBOptions.PATH, path)
      .option(GDBOptions.NAME, name)
      .option(GDBOptions.NUM_PARTITIONS, numPartitions)
      .option(GDBOptions.WKID, wkid)
      .load()
  }

  implicit class ByteBufferImplicits(byteBuffer: ByteBuffer) {

    implicit def getVarUInt() = {
      var shift = 7
      var b: Long = byteBuffer.get
      var ret = b & 0x7FL
      var old = ret
      while ((b & 0x80L) != 0) {
        b = byteBuffer.get
        ret = ((b & 0x7FL) << shift) | old
        old = ret
        shift += 7
      }
      ret
    }

    implicit def getVarInt() = {
      var shift = 7
      var b: Long = byteBuffer.get
      val isNeg = (b & 0x40L) != 0
      var ret = b & 0x3FL
      var old = ret
      while ((b & 0x80L) != 0) {
        b = byteBuffer.get
        ret = ((b & 0x7FL) << (shift - 1)) | old
        old = ret
        shift += 7
      }
      if (isNeg) -ret else ret
    }
  }

}
