package com.github.lbhat1.mlstorm.streaming.spout.simpletext;

import backtype.storm.task.TopologyContext;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;
import storm.trident.operation.TridentCollector;
import storm.trident.spout.ITridentSpout;
import storm.trident.spout.RichSpoutBatchExecutor;
import storm.trident.topology.TransactionAttempt;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

 /*
 * Copyright 2013-2015 Lakshmisha Bhat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * A quick "transactional" text file spout: emits one tuple with one single field for each line in the file.
 * <p/>
 * <p/>
 * Watch out that:
 * <p/>
 * It might consume a lot of memory:
 * full payload of the emitted messages is kept in memory until the message are successfully ack'ed
 * <p/>
 * It's not restart proof: all state in maintained in memory as 2 static hashmap => if the worker
 * should restart, it would just replays the file from memory. One would need
 * to keep the emittedMessages and txidMsgIds state persisted somewhere to fix that...
 * <p/>
 * It's not very robust: failure while reading just bubble up to the framework (which *might* be ok
 * if we were restart-proof...)
 * Reading a local file makes this spout not partitioned!
 */
public class TransactionalTextFileSpout implements ITridentSpout<Set<String>> {

    private static final long serialVersionUID = 1L;
    // Set of messages ids emitted for each transaction id
    private final static Map<Long, Set<String>> txidMsgIds = new HashMap<Long, Set<String>>();
    // full String payload for each message id
    private final static Map<String, String> emittedMessages = new HashMap<String, String>();
    private final String singleOutputFieldName;
    private final String encoding;
    private final String sourceFileName;

    public TransactionalTextFileSpout(String singleOutputFieldName, String sourceFileName, String encoding) {
        this.singleOutputFieldName = singleOutputFieldName;
        this.sourceFileName = sourceFileName;
        this.encoding = encoding;
    }

    // there's no synchronization one the access to the reader nor to the 2 State maps: Coordinator is executed by a single thread
    private class Coordinator implements BatchCoordinator<Set<String>> {

        private final long batchSize;
        private BufferedReader reader;

        public Coordinator(long batchSize) {
            this.batchSize = batchSize;
        }

        private void initIfNeeded() {
            if (reader == null) {
                try {
                    reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(sourceFileName)), encoding));
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to initialize spout", e);
                }
            }
        }

        //@Override
        public Set<String> initializeTransaction(long txid, Set<String> prevMetadata) {
            initIfNeeded();

            // the initialization is doing the actual read operation and keeping the result in memory,
            // to be emitted by the emitter below

            try {
                Set<String> emittedIds = new HashSet<String>();
                for (int idx = 0; idx < batchSize; idx++) {
                    String rawLine = reader.readLine();
                    if (rawLine != null) {
                        try {
                            String messageId = UUID.randomUUID().toString();
                            emittedIds.add(messageId);
                            emittedMessages.put(messageId, rawLine);
                        } catch (Exception e) {
                            throw new RuntimeException(MessageFormat.format("failed to read file {0}", sourceFileName), e);
                        }
                    } else {
                        Logger.getAnonymousLogger().log(Level.INFO, "sleep");
                        Utils.sleep(5 /*magic!!*/);
                    }
                }

                return emittedIds;

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        @Override
        public Set<String> initializeTransaction(long txid, Set<String> prevMetadata, Set<String> currMetadata) {
            return initializeTransaction(txid, prevMetadata);
        }

        @Override
        public void success(long txid) {
            Set<String> emittedMsgIds = txidMsgIds.get(txid);
            if (emittedMsgIds != null) {
                for (String messageId : emittedMsgIds) {
                    emittedMessages.remove(messageId);
                }
            }
            txidMsgIds.remove(txid);
        }

        @Override
        public boolean isReady(long txid) {
            return true;
        }

        @Override
        public void close() {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new IllegalStateException(MessageFormat.format("failed to close file from {0} => giving up...", TransactionalTextFileSpout.class), e);
                }
            }
        }

    }

    private class TextEmitter implements Emitter<Set<String>> {

        @Override
        public void emitBatch(TransactionAttempt tx, Set<String> coordinatorMeta, TridentCollector collector) {
            if (tx == null || coordinatorMeta == null || collector == null) {
                return;
            }

            for (String messageId : txidMsgIds.get(tx.getTransactionId())) {
                assert emittedMessages.containsKey(messageId);
                String payload = emittedMessages.get(messageId);
                collector.emit(new Values(payload));
            }
        }

        @Override
        public void success(TransactionAttempt tx) {
            // NOP
        }

        @Override
        public void close() {
            // NOP
        }

    }

    @Override
    public BatchCoordinator<Set<String>> getCoordinator(String txStateId, Map conf, TopologyContext context) {
        return new Coordinator((Long) conf.get(RichSpoutBatchExecutor.MAX_BATCH_SIZE_CONF));
    }

    @Override
    public Emitter<Set<String>> getEmitter(String txStateId, Map conf, TopologyContext context) {
        return new TextEmitter();
    }

    @Override
    public Map getComponentConfiguration() {
        return null;
    }

    @Override
    public Fields getOutputFields() {
        return new Fields(singleOutputFieldName);
    }

}
