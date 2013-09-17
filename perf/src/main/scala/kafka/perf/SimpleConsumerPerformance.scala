/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kafka.perf

import java.net.URI
import kafka.utils._
import kafka.consumer.SimpleConsumer
import org.apache.log4j.Logger
import kafka.api.{OffsetRequest, FetchRequest}
import java.text.SimpleDateFormat

/**
 * Performance test for the simple consumer
 */
object SimpleConsumerPerformance {

  def main(args: Array[String]) {
    val logger = Logger.getLogger(getClass)
    val config = new ConsumerPerfConfig(args)

    if(!config.hideHeader) {
      if(!config.showDetailedStats)
        println("start.time, end.time, fetch.size, data.consumed.in.MB, MB.sec, data.consumed.in.nMsg, nMsg.sec")
      else
        println("time, fetch.size, data.consumed.in.MB, MB.sec, data.consumed.in.nMsg, nMsg.sec")
    }

    val consumer = new SimpleConsumer(config.url.getHost, config.url.getPort, 30*1000, 2*config.fetchSize)

    // reset to latest or smallest offset
    var offset: Long = if(config.fromLatest) consumer.getOffsetsBefore(config.topic, config.partition, OffsetRequest.LatestTime, 1).head
                       else consumer.getOffsetsBefore(config.topic, config.partition, OffsetRequest.EarliestTime, 1).head

    val startMs = System.currentTimeMillis
    var done = false
    var totalBytesRead = 0L
    var totalMessagesRead = 0L
    var consumedInterval = 0
    var lastReportTime: Long = startMs
    var lastBytesRead = 0L
    var lastMessagesRead = 0L
    while(!done) {
      val messages = consumer.fetch(new FetchRequest(config.topic, config.partition, offset, config.fetchSize))
      var messagesRead = 0
      var bytesRead = 0

      for(message <- messages) {
        messagesRead += 1
        bytesRead += message.message.payloadSize
      }
      
      if(messagesRead == 0 || totalMessagesRead > config.numMessages)
        done = true
      else
        offset += messages.validBytes
      
      totalBytesRead += bytesRead
      totalMessagesRead += messagesRead
      consumedInterval += messagesRead
      
      if(consumedInterval > config.reportingInterval) {
        if(config.showDetailedStats) {
          val reportTime = System.currentTimeMillis
          val elapsed = (reportTime - lastReportTime)/1000.0
          val totalMBRead = ((totalBytesRead-lastBytesRead)*1.0)/(1024*1024)
          println(("%s, %d, %.4f, %.4f, %d, %.4f").format(config.dateFormat.format(reportTime), config.fetchSize,
            (totalBytesRead*1.0)/(1024*1024), totalMBRead/elapsed,
            totalMessagesRead, (totalMessagesRead-lastMessagesRead)/elapsed))
        }
        lastReportTime = SystemTime.milliseconds
        lastBytesRead = totalBytesRead
        lastMessagesRead = totalMessagesRead
        consumedInterval = 0
      }
    }
    val reportTime = System.currentTimeMillis
    val elapsed = (reportTime - startMs) / 1000.0

    if(!config.showDetailedStats) {
      val totalMBRead = (totalBytesRead*1.0)/(1024*1024)
      println(("%s, %s, %d, %.4f, %.4f, %d, %.4f").format(config.dateFormat.format(startMs),
        config.dateFormat.format(reportTime), config.fetchSize, totalMBRead, totalMBRead/elapsed,
        totalMessagesRead, totalMessagesRead/elapsed))
    }
    System.exit(0)
  }

  class ConsumerPerfConfig(args: Array[String]) extends PerfConfig(args) {
    val urlOpt = parser.accepts("server", "REQUIRED: The hostname of the server to connect to.")
                           .withRequiredArg
                           .describedAs("kafka://hostname:port")
                           .ofType(classOf[String])
    val resetBeginningOffsetOpt = parser.accepts("from-latest", "If the consumer does not already have an established " +
      "offset to consume from, start with the latest message present in the log rather than the earliest message.")
    val partitionOpt = parser.accepts("partition", "The topic partition to consume from.")
                           .withRequiredArg
                           .describedAs("partition")
                           .ofType(classOf[java.lang.Integer])
                           .defaultsTo(0)
    val fetchSizeOpt = parser.accepts("fetch-size", "REQUIRED: The fetch size to use for consumption.")
                           .withRequiredArg
                           .describedAs("bytes")
                           .ofType(classOf[java.lang.Integer])
                           .defaultsTo(1024*1024)

    val options = parser.parse(args : _*)

    for(arg <- List(topicOpt, urlOpt)) {
      if(!options.has(arg)) {
        System.err.println("Missing required argument \"" + arg + "\"")
        parser.printHelpOn(System.err)
        System.exit(1)
      }
    }
    val url = new URI(options.valueOf(urlOpt))
    val fetchSize = options.valueOf(fetchSizeOpt).intValue
    val fromLatest = options.has(resetBeginningOffsetOpt)
    val partition = options.valueOf(partitionOpt).intValue
    val topic = options.valueOf(topicOpt)
    val numMessages = options.valueOf(numMessagesOpt).longValue
    val reportingInterval = options.valueOf(reportingIntervalOpt).intValue
    val showDetailedStats = options.has(showDetailedStatsOpt)
    val dateFormat = new SimpleDateFormat(options.valueOf(dateFormatOpt))
    val hideHeader = options.has(hideHeaderOpt)
  }
}
