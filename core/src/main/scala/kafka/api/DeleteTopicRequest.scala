package kafka.api

import kafka.network.{Send, Request}
import java.nio.ByteBuffer
import kafka.utils.{nonthreadsafe, Utils}
import java.nio.channels.GatheringByteChannel
import kafka.common.ErrorMapping

object DeleteTopicRequest {
  def readFrom(buffer: ByteBuffer): DeleteTopicRequest = {
    val topic = Utils.readShortString(buffer, "UTF-8")
    val partition = buffer.getInt()
    new DeleteTopicRequest(topic, partition)
  }
}


class DeleteTopicRequest(val topic: String,
                         val partition: Int) extends Request(RequestKeys.DeleteTopic) {
  def sizeInBytes: Int = 2 + topic.length + 4

  def writeTo(buffer: ByteBuffer) {
    Utils.writeShortString(buffer, topic, "UTF-8")
    buffer.putInt(partition)
  }

  override def toString: String = "DeleteTopicRequest(topic: %s, partition: %d".format(topic, partition)
}


@nonthreadsafe
private[kafka] class DeleteSuccessSend(success: Boolean) extends Send {

  private val header = ByteBuffer.allocate(6)
  header.putInt(2)
  if (success) {
    header.putShort(ErrorMapping.NoError.asInstanceOf[Short])
  } else {
    header.putShort(ErrorMapping.IllegalArgumentCode.asInstanceOf[Short])
  }
  header.rewind()


  def writeTo(channel: GatheringByteChannel): Int = {
    var written = 0
    if (header.hasRemaining)
      written += channel.write(header)

    if (!header.hasRemaining)
      complete = true

    written
  }

  var complete: Boolean = false
}