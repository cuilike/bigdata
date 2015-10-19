package com.github.lbhat1.mlstorm.streaming.drpc;

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

import backtype.storm.generated.DRPCExecutionException;
import backtype.storm.utils.DRPCClient;
import org.apache.thrift7.TException;

import java.io.IOException;

/**
 * Simple utility class to run drpc queries
 */
public class GeneralDrpcQueryRunner {
    public static void main(final String[] args) throws IOException, TException, DRPCExecutionException {
        if (args.length < 3) {
            System.err.println("Where are the arguments? args -- DrpcServer DrpcFunctionName parameters");
            return;
        }

        final DRPCClient client = new DRPCClient(args[0], 3772, 1000000 /*timeout*/);
        System.err.println(runQuery(args[1], args[2], client));
        client.close();
    }

    private static String runQuery(final String topologyAndDrpcServiceName, final String args, final DRPCClient client) throws
            TException,
            DRPCExecutionException {
        return client.execute(topologyAndDrpcServiceName, args);
    }
}

