package kafka.common

import kafka.utils.{SystemTime, Logging}
import kafka.network.{BoundedByteBufferSend, Request, Receive}
import kafka.api.DeleteTopicRequest


class Cleaner(val host: String, val port: Int) extends Logging {

//  private val lock = new Object()
//
//  def deleteTopicForPartition(request: DeleteTopicRequest) {
//    lock synchronized {
//      val startTime = SystemTime.nanoseconds
//
//      var response: Tuple2[Receive, Int] = null
//      try {
//
//      }
//    }
//  }
//
//  private def sendRequest(request: Request) = {
//    val send = new BoundedByteBufferSend(request)
//    send.writeCompletely(channel)
//  }

}
