package com.esri.gdb

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.sql.Timestamp

import org.apache.spark.sql.types.StructField

trait GDBField extends Serializable {
  type T

  def field(): StructField

  def name(): String = field.name

  def nullable(): Boolean = field.nullable

  def readValue(byteBuffer: ByteBuffer, oid: Int): T
}

class FieldFloat32(val field: StructField) extends GDBField {
  override type T = Float

  override def readValue(byteBuffer: ByteBuffer, oid: Int): Float = {
    byteBuffer.getFloat
  }
}

class FieldFloat64(val field: StructField) extends GDBField {
  override type T = Double

  override def readValue(byteBuffer: ByteBuffer, oid: Int): Double = {
    byteBuffer.getDouble
  }
}

class FieldInt16(val field: StructField) extends GDBField {
  override type T = Short

  override def readValue(byteBuffer: ByteBuffer, oid: Int): Short = {
    byteBuffer.getShort
  }
}

class FieldInt32(val field: StructField) extends GDBField {
  override type T = Int

  override def readValue(byteBuffer: ByteBuffer, oid: Int): Int = {
    byteBuffer.getInt
  }
}

class FieldUUID(val field: StructField) extends GDBField {
  override type T = String

  private val b = new Array[Byte](16)

  override def readValue(byteBuffer: ByteBuffer, oid: Int): String = {
    var n = 0
    while (n < 16) {
      b(n) = byteBuffer.get
      n += 1
    }
    "{%02X%02X%02X%02X-%02X%02X-%02X%02X-%02X%02X-%02X%02X%02X%02X%02X%02X}".format(
      b(3), b(2), b(1), b(0),
      b(5), b(4), b(7), b(6),
      b(8), b(9), b(10), b(11),
      b(12), b(13), b(14), b(15))
  }
}

class FieldDateTime(val field: StructField) extends GDBField {
  override type T = Timestamp

  override def readValue(byteBuffer: ByteBuffer, oid: Int): Timestamp = {
    val numDays = byteBuffer.getDouble
    // convert days since 12/30/1899 to 1/1/1970
    val unixDays = numDays - 25569
    val millis = (unixDays * 1000 * 60 * 60 * 24).ceil.toLong
    new Timestamp(millis)
  }
}

class FieldOID(val field: StructField) extends GDBField {
  override type T = Int

  override def readValue(byteBuffer: ByteBuffer, oid: Int): Int = oid
}

abstract class FieldBytes extends GDBField {
  protected var m_bytes = new Array[Byte](1024)

  def fillVarBytes(byteBuffer: ByteBuffer): Int = {
    val numBytes = byteBuffer.getVarUInt.toInt
    if (numBytes > m_bytes.length) {
      m_bytes = new Array[Byte](numBytes)
    }
    var n = 0
    while (n < numBytes) {
      m_bytes(n) = byteBuffer.get
      n += 1
    }
    numBytes
  }

  def getByteBuffer(byteBuffer: ByteBuffer): ByteBuffer = {
    val numBytes = fillVarBytes(byteBuffer)
    ByteBuffer.wrap(m_bytes, 0, numBytes)
  }

}

class FieldBinary(val field: StructField) extends FieldBytes {
  override type T = ByteBuffer

  override def readValue(byteBuffer: ByteBuffer, oid: Int): ByteBuffer = {
    getByteBuffer(byteBuffer)
  }
}

class FieldString(val field: StructField) extends FieldBytes {
  override type T = String

  override def readValue(byteBuffer: ByteBuffer, oid: Int): String = {
    val numBytes = fillVarBytes(byteBuffer)
    new String(m_bytes, 0, numBytes, StandardCharsets.UTF_8)
  }
}

class FieldGeomNoop(val field: StructField) extends FieldBytes {
  override type T = String

  override def readValue(byteBuffer: ByteBuffer, oid: Int): String = {
    throw new RuntimeException("Should not have a NOOP geometry !")
  }
}