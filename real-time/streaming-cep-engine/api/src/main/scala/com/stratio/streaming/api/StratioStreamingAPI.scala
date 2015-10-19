/**
 * Copyright (C) 2014 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.streaming.api

import java.util.List

import com.stratio.streaming.api.messaging.MessageBuilder.builder
import com.stratio.streaming.api.messaging.{ColumnNameType, ColumnNameValue, MessageBuilderWithColumns}
import com.stratio.streaming.api.zookeeper.ZookeeperConsumer
import com.stratio.streaming.commons.constants.InternalTopic
import com.stratio.streaming.commons.constants.STREAMING.{ZK_EPHEMERAL_NODE_STATUS_PATH, ZK_EPHEMERAL_NODE_STATUS_CONNECTED, ZK_EPHEMERAL_NODE_STATUS_INITIALIZED}
import com.stratio.streaming.commons.constants.STREAM_OPERATIONS.ACTION.{INDEX, LISTEN, SAVETO_CASSANDRA, SAVETO_MONGO, SAVETO_SOLR, STOP_INDEX, STOP_LISTEN, STOP_SAVETO_CASSANDRA, STOP_SAVETO_MONGO, STOP_SAVETO_SOLR}
import com.stratio.streaming.commons.constants.STREAM_OPERATIONS.DEFINITION
import com.stratio.streaming.commons.constants.STREAM_OPERATIONS.DEFINITION.{ADD_QUERY, ALTER, DROP, REMOVE_QUERY}
import com.stratio.streaming.commons.constants.STREAM_OPERATIONS.MANIPULATION.LIST
import com.stratio.streaming.commons.exceptions.{StratioEngineConnectionException, StratioEngineOperationException, StratioEngineStatusException}
import com.stratio.streaming.commons.kafka.service.{KafkaTopicService, TopicService}
import com.stratio.streaming.commons.messages.ColumnNameTypeValue
import com.stratio.streaming.commons.streams.StratioStream
import com.stratio.streaming.dto.StratioQueryStream
import com.stratio.streaming.kafka.{KafkaConsumer, KafkaProducer}
import com.stratio.streaming.messaging.{InsertMessageBuilder, QueryMessageBuilder, StreamMessageBuilder}
import org.apache.curator.framework.api.CuratorEventType.WATCHED
import org.apache.curator.framework.api.{CuratorEvent, CuratorListener}
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryOneTime
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions.{asScalaBuffer, bufferAsJavaList}

class StratioStreamingAPI
  extends IStratioStreamingAPI {

  import com.stratio.streaming.api.StratioStreamingAPI._

  def createStream(streamName: String, columns: List[ColumnNameType]) = {
    checkStreamingStatus()
    val operation = DEFINITION.CREATE.toLowerCase
    val creationStreamMessage = MessageBuilderWithColumns(sessionId, operation).build(streamName, columns)
    syncOperation.performSyncOperation(creationStreamMessage)
  }

  def alterStream(streamName: String, columns: List[ColumnNameType]) = {
    checkStreamingStatus()
    val operation = ALTER.toLowerCase
    val alterStreamMessage = MessageBuilderWithColumns(sessionId, operation).build(streamName, columns)
    syncOperation.performSyncOperation(alterStreamMessage)
  }

  def insertData(streamName: String, data: List[ColumnNameValue]) = {
    checkStreamingStatus()
    val insertStreamMessage = InsertMessageBuilder(sessionId).build(streamName, data)
    asyncOperation.performAsyncOperation(insertStreamMessage)
  }

  def addQuery(streamName: String, query: String): String = {
    checkStreamingStatus()
    val operation = ADD_QUERY.toLowerCase
    val addQueryStreamMessage = QueryMessageBuilder(sessionId).build(streamName, query, operation)
    syncOperation.performSyncOperation(addQueryStreamMessage)
    getQueryId(streamName, query)
  }

  def getQueryId(streamName: String, query: String): String = {
    val queries = queriesFromStream(streamName)
    val addedQuery = queries.find(theQuery => theQuery.query.equals(query))
    addedQuery match {
      case Some(q) => q.queryId
      case _ => ""
    }
  }

  def removeQuery(streamName: String, queryId: String) = {
    checkStreamingStatus()
    val operation = REMOVE_QUERY.toLowerCase
    val removeQueryMessage = QueryMessageBuilder(sessionId).build(streamName, queryId, operation)
    syncOperation.performSyncOperation(removeQueryMessage)
  }

  def dropStream(streamName: String) = {
    checkStreamingStatus()
    val operation = DROP.toLowerCase
    val dropStreamMessage = StreamMessageBuilder(sessionId).build(streamName, operation)
    syncOperation.performSyncOperation(dropStreamMessage)
  }

  def listenStream(streamName: String) = {
    checkStreamingStatus()
    val operation = LISTEN.toLowerCase
    val listenStreamMessage = StreamMessageBuilder(sessionId).build(streamName, operation)
    syncOperation.performSyncOperation(listenStreamMessage)
    val kafkaConsumer = new KafkaConsumer(streamName, zookeeperCluster, readFromStartOfStream = false)
    streamingListeners.put(streamName, kafkaConsumer)
    kafkaConsumer.stream
  }

  def stopListenStream(streamName: String) = {
    checkStreamingStatus()
    val operation = STOP_LISTEN.toLowerCase
    val stopListenStreamMessage = StreamMessageBuilder(sessionId).build(streamName, operation)
    shutdownKafkaConsumerAndRemoveStreamingListener(streamName)
    syncOperation.performSyncOperation(stopListenStreamMessage)
  }

  private def shutdownKafkaConsumerAndRemoveStreamingListener(streamName: String) {
    val kafkaConsumer = streamingListeners.get(streamName)
    kafkaConsumer match {
      case Some(consumer) => consumer.close()
      case _ => //
    }
    streamingListeners.remove(streamName)
  }

  def queriesFromStream(stream: String): List[StratioQueryStream] = {
    val stratioStreams = listStreams().toList
    val stratioStream = stratioStreams.find(element => element.getStreamName.equals(stream))
    stratioStream match {
      case None => throw new StratioEngineOperationException("StratioEngine error: STREAM DOES NOT EXIST")
      case Some(element) => element.getQueries.map(query => new StratioQueryStream(query.getQuery, query.getQueryId))
    }
  }

  def columnsFromStream(stream: String): List[ColumnNameTypeValue] = {
    val stratioStreams = listStreams().toList
    val stratioStream = stratioStreams.find(element => element.getStreamName.equals(stream))
    stratioStream match {
      case None => throw new StratioEngineOperationException("StratioEngine error: STREAM DOES NOT EXIST")
      case Some(element) => element.getColumns
    }
  }

  def listStreams(): List[StratioStream] = {
    checkStreamingStatus()
    val operation = LIST.toLowerCase
    val listStreamMessage = builder.withOperation(operation)
      .withSessionId(sessionId)
      .build()
    statusOperation.getListStreams(listStreamMessage)
  }

  def saveToCassandra(streamName: String) = {
    checkStreamingStatus()
    val operation = SAVETO_CASSANDRA.toLowerCase
    val saveToCassandraMessage = StreamMessageBuilder(sessionId).build(streamName, operation)
    syncOperation.performSyncOperation(saveToCassandraMessage)
  }

  def stopSaveToCassandra(streamName: String) = {
    checkStreamingStatus()
    val operation = STOP_SAVETO_CASSANDRA.toLowerCase
    val stopSaveToCassandraMessage = StreamMessageBuilder(sessionId).build(streamName, operation)
    syncOperation.performSyncOperation(stopSaveToCassandraMessage)
  }

  def saveToMongo(streamName: String) = {
    checkStreamingStatus()
    val operation = SAVETO_MONGO.toLowerCase
    val saveToCassandraMessage = StreamMessageBuilder(sessionId).build(streamName, operation)
    syncOperation.performSyncOperation(saveToCassandraMessage)
  }

  def stopSaveToMongo(streamName: String) = {
    checkStreamingStatus()
    val operation = STOP_SAVETO_MONGO.toLowerCase
    val stopSaveToCassandraMessage = StreamMessageBuilder(sessionId).build(streamName, operation)
    syncOperation.performSyncOperation(stopSaveToCassandraMessage)
  }

  def saveToSolr(streamName: String) = {
    checkStreamingStatus()
    val operation = SAVETO_SOLR.toLowerCase
    val saveToSolrMEssage = StreamMessageBuilder(sessionId).build(streamName, operation)
    syncOperation.performSyncOperation(saveToSolrMEssage)
  }

  def stopSaveToSolr(streamName: String) = {
    checkStreamingStatus()
    val operation = STOP_SAVETO_SOLR.toLowerCase
    val stopSaveToSolrMessage = StreamMessageBuilder(sessionId).build(streamName, operation)
    syncOperation.performSyncOperation(stopSaveToSolrMessage)
  }

  def indexStream(streamName: String) = {
    checkStreamingStatus()
    val operation = INDEX.toLowerCase
    val indexStreamMessage = StreamMessageBuilder(sessionId).build(streamName, operation)
    syncOperation.performSyncOperation(indexStreamMessage)
  }

  def stopIndexStream(streamName: String) = {
    checkStreamingStatus()
    val operation = STOP_INDEX.toLowerCase
    val indexStreamMessage = StreamMessageBuilder(sessionId).build(streamName, operation)
    syncOperation.performSyncOperation(indexStreamMessage)
  }

  def initialize() = {
    try {
      val brokerServer = config.getString("kafka.server")
      val brokerPort = config.getInt("kafka.port")
      kafkaCluster = s"$brokerServer:$brokerPort"

      val theZookeeperServer = config.getString("zookeeper.server")
      val zookeeperPort = config.getInt("zookeeper.port")
      zookeeperServer = s"$theZookeeperServer:$zookeeperPort"

      log.info("Establishing connection with the engine...")
      checkEphemeralNode()
      startEphemeralNodeWatch()
      log.info("Initializing kafka topic...")
      initializeTopic()
      this
    } catch {
      case ex => throw new StratioEngineConnectionException("Unable to connect to stratio streaming. " + ex.getMessage)
    }
  }

  def initializeWithServerConfig(kafkaServer: String,
                                 kafkaPort: Int,
                                 theZookeeperServer: String,
                                 theZookeeperPort: Int) = {
    try {
      kafkaCluster = s"$kafkaServer:$kafkaPort"
      zookeeperServer = s"$theZookeeperServer:$theZookeeperPort"

      log.info("Establishing connection with the engine...")
      checkEphemeralNode()
      startEphemeralNodeWatch()
      log.info("Initializing kafka topic...")
      initializeTopic()
      this
    } catch {
      case ex => throw new StratioEngineConnectionException("Unable to connect to stratio streaming. " + ex.getMessage)
    }
  }

  override def withServerConfig(kafkaQuorum: String, zookeeperQuorum: String): IStratioStreamingAPI = {
    kafkaCluster = kafkaQuorum
    zookeeperServer = zookeeperQuorum
    this
  }

  override def withServerConfig(kafkaHost: String,
                                kafkaPort: Int,
                                zookeeperHost: String,
                                zookeeperPort: Int): IStratioStreamingAPI = {

    kafkaCluster = s"$kafkaHost:$kafkaPort"
    zookeeperServer = s"$zookeeperHost:$zookeeperPort"
    this
  }

  override def init(): IStratioStreamingAPI = {
    try {
      log.info("Establishing connection with the engine...")
      checkEphemeralNode()
      startEphemeralNodeWatch()
      log.info("Initializing kafka topic...")
      initializeTopic()
      this
    } catch {
      case ex => throw new StratioEngineConnectionException("Unable to connect to stratio streaming. " + ex.getMessage)
    }
  }

  override def isInit(): Boolean = streamingUp && streamingRunning

  override def isConnected(): Boolean = {
    return streamingUp && streamingRunning
  }

  def defineAcknowledgeTimeOut(timeOutInMs: Int) = {
    ackTimeOut = timeOutInMs
    this
  }

  override def close: Unit = {
    kafkaProducer.close()
    kafkaDataProducer.close()
    topicService.close()
    zookeeperClient.close()
  }

  val streamingTopicName = InternalTopic.TOPIC_REQUEST.getTopicName();
  val streamingDataTopicName = InternalTopic.TOPIC_DATA.getTopicName();
  val sessionId = "" + System.currentTimeMillis()

  //TODO refactor ASAP
  lazy val consumerBrokerServer = kafkaCluster.split(",")(0).split(":")(0)
  lazy val consumerBrokerPort = kafkaCluster.split(",")(0).split(":")(1).toInt

  var kafkaCluster = ""
  lazy val kafkaBroker = s"$kafkaCluster"
  var zookeeperServer = ""
  lazy val zookeeperCluster = s"$zookeeperServer"
  var streamingUp = false
  var streamingRunning = false
  val streamingListeners = scala.collection.mutable.Map[String, KafkaConsumer]()
  lazy val kafkaProducer = new KafkaProducer(InternalTopic.TOPIC_REQUEST.getTopicName(), kafkaBroker)
  lazy val kafkaDataProducer = new KafkaProducer(InternalTopic.TOPIC_DATA.getTopicName(), kafkaBroker)
  val retryPolicy = new RetryOneTime(500)
  lazy val zookeeperClient = CuratorFrameworkFactory.newClient(zookeeperCluster, retryPolicy)
  var topicService: TopicService = _
  var ackTimeOut = 8000
  lazy val zookeeperConsumer = {
    zookeeperClient.start()
    ZookeeperConsumer(zookeeperClient)
  }
  lazy val syncOperation = new StreamingAPISyncOperation(kafkaProducer, zookeeperConsumer, ackTimeOut)
  lazy val asyncOperation = new StreamingAPIAsyncOperation(kafkaDataProducer)
  lazy val statusOperation = new StreamingAPIListOperation(kafkaProducer, zookeeperConsumer, ackTimeOut)

  private def checkEphemeralNode() {
    val ephemeralNodePath = ZK_EPHEMERAL_NODE_STATUS_PATH
    if (!zookeeperConsumer.zNodeExists(ephemeralNodePath)) {
      log.warn("Ephemeral node does not exist")
      throw new StratioEngineStatusException("Can't connect. Check if Stratio streaming is down or connection to " +
        "Zookeeper")
    } else {
      val streamingStatus = zookeeperConsumer.getZNodeData(ZK_EPHEMERAL_NODE_STATUS_PATH).get
      streamingStatus match {
        case ZK_EPHEMERAL_NODE_STATUS_CONNECTED =>
          streamingUp = true
          streamingRunning = false
        case ZK_EPHEMERAL_NODE_STATUS_INITIALIZED =>
          streamingUp = true
          streamingRunning = true
      }
    }
  }

  private def startEphemeralNodeWatch() {
    zookeeperClient.checkExists().watched().forPath(ZK_EPHEMERAL_NODE_STATUS_PATH)
    addListener()
  }

  private def initializeTopic() {
    topicService = new KafkaTopicService(zookeeperCluster, consumerBrokerServer, consumerBrokerPort, 10000, 10000)
    topicService.createTopicIfNotExist(streamingTopicName, 1, 1)
    topicService.createTopicIfNotExist(streamingDataTopicName, 1, 1);
  }

  private def checkStreamingStatus() {
    if (!streamingUp) throw new StratioEngineStatusException("Stratio streaming is down")
    if (!streamingRunning) throw new StratioEngineStatusException("Stratio streaming not yet initialized")
  }

  private def addListener() = {
    zookeeperClient.getCuratorListenable().addListener(new CuratorListener() {
      def eventReceived(client: CuratorFramework, event: CuratorEvent) = {
        event.getType() match {
          case WATCHED => {
            zookeeperClient.checkExists().watched().forPath(ZK_EPHEMERAL_NODE_STATUS_PATH)
            zookeeperConsumer.getZNodeData(ZK_EPHEMERAL_NODE_STATUS_PATH) match {
              case Some(ZK_EPHEMERAL_NODE_STATUS_CONNECTED) => streamingUp = true
              case Some(ZK_EPHEMERAL_NODE_STATUS_INITIALIZED) => streamingUp = false
              case _ => {
                streamingUp = false
                streamingRunning = false
              }
            }
          }
          case x => {
            log.debug("Unused curatorEvent {}", x)
          }
        }
      }
    })
  }
}

object StratioStreamingAPI
  extends StratioStreamingAPIConfig {
  lazy val log = LoggerFactory.getLogger(getClass)
}
