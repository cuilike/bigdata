/**
 * Copyright (C) 2014 Stratio (http://stratio.com)
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.streaming.service;

import com.stratio.streaming.commons.constants.StreamAction;
import com.stratio.streaming.commons.messages.StreamQuery;
import com.stratio.streaming.dao.StreamStatusDao;
import com.stratio.streaming.dao.StreamingFailoverDao;
import com.stratio.streaming.model.FailoverPersistenceStoreModel;
import com.stratio.streaming.streams.QueryDTO;
import com.stratio.streaming.streams.StreamStatusDTO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;

public class StreamingFailoverService {

    private final StreamStatusDao streamStatusDao;
    private final StreamMetadataService streamMetadataService;
    private final StreamingFailoverDao streamingFailoverDao;

    @Autowired
    private StreamOperationService streamOperationService;


    public StreamingFailoverService(StreamStatusDao streamStatusDao, StreamMetadataService streamMetadataService,
                                    StreamingFailoverDao streamingFailoverDao) {
        this.streamStatusDao = streamStatusDao;
        this.streamMetadataService = streamMetadataService;
        this.streamingFailoverDao = streamingFailoverDao;
    }

    public synchronized void load() throws Exception {
        FailoverPersistenceStoreModel failoverPersistenceStoreModel = streamingFailoverDao.load();
        if (failoverPersistenceStoreModel != null) {
            streamStatusDao.putAll(failoverPersistenceStoreModel.getStreamStatuses());
            Map<String, StreamStatusDTO> streamsStatus = failoverPersistenceStoreModel.getStreamStatuses();
            for (Map.Entry<String, StreamStatusDTO> entry : streamsStatus.entrySet()) {
                StreamStatusDTO stream = entry.getValue();
                streamOperationService.createStream(stream.getStreamName(), stream.getStreamDefinition());
                for (Map.Entry<String, QueryDTO> query : stream.getAddedQueries().entrySet()) {
                    streamOperationService.addQuery(entry.getKey(), query.getValue().getQueryRaw());
                }
                for (StreamAction action : stream.getActionsEnabled()) {
                    streamOperationService.enableAction(entry.getKey(), action);
                }
            }
//            streamMetadataService.setSnapshot(failoverPersistenceStoreModel.getSiddhiSnapshot());
        }
    }

    public synchronized void save() throws Exception {
//        streamingFailoverDao.save(new FailoverPersistenceStoreModel(streamStatusDao.getAll(), streamMetadataService
//                .getSnapshot()));
        streamingFailoverDao.save(new FailoverPersistenceStoreModel(streamStatusDao.getAll(), null));
    }

}
