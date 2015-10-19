/*
 * Copyright (C) 2014 The Calrissian Authors
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
package org.calrissian.flowmix.api.builder;

import java.util.ArrayList;
import java.util.List;

import org.calrissian.flowmix.core.model.op.PartitionOp;

public class PartitionBuilder extends AbstractOpBuilder {

    private List<String> fieldsList = new ArrayList<String>();

    public PartitionBuilder(StreamBuilder flowOpsBuilder) {
        super(flowOpsBuilder);
    }

    public PartitionBuilder fields(String... fields) {
      for(String field : fields)
        fieldsList.add(field);
      return this;
    }

    public StreamBuilder end() {
      /**
       * It's possible that if a partitioner does not have any specified fieldsList, that it uses a default partition.
       */
      getStreamBuilder().addFlowOp(new PartitionOp(fieldsList));
      return getStreamBuilder();
    }
}
