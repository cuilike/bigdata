package com.datastax;

import com.datastax.driver.core.*;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.ProtocolOptions.Compression;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CassClient {
    private static Logger    log                        = LoggerFactory.getLogger(CassClient.class);

    private Cluster          cluster;
    private Session          session;
    private String           keyspace;
    private String[]         agentHosts;
    private String           localDc;

    private ConsistencyLevel consistencyLevel           = ConsistencyLevel.SERIAL;

    public CassClient(String keyspace, String agentHosts, String localDc) {
        this.keyspace = keyspace;
        this.agentHosts = agentHosts.split(",");
        this.localDc = localDc;
    }

    public void setConsistency(String consistency) {
        consistencyLevel = Enum.valueOf(ConsistencyLevel.class, consistency);
    }

    public void init() {
        Builder builder = Cluster.builder();
        for (String host : this.agentHosts) {
            builder.addContactPoints(host);
        }
        builder.withPort(9042);

        cluster = builder.build();
        session = cluster.connect(keyspace);
    }
    public void init2(){
        Builder builder = Cluster.builder();

        PoolingOptions poolingOptions = new PoolingOptions().setMaxConnectionsPerHost(HostDistance.REMOTE, 20).setCoreConnectionsPerHost(HostDistance.REMOTE, 2);
        SocketOptions socketOptions = new SocketOptions().setKeepAlive(true).setReceiveBufferSize(1024 * 1024).setSendBufferSize(1024 * 1024).setConnectTimeoutMillis(5 * 1000).setReadTimeoutMillis(1000);
        QueryOptions queryOptions = new QueryOptions().setFetchSize(1000).setConsistencyLevel(consistencyLevel);
        DCAwareRoundRobinPolicy dCAwareRoundRobinPolicy = new DCAwareRoundRobinPolicy(localDc, 0);

        builder.withPoolingOptions(poolingOptions);
        builder.withSocketOptions(socketOptions);
        builder.withQueryOptions(queryOptions);
        builder.withRetryPolicy(DefaultRetryPolicy.INSTANCE);
        builder.withLoadBalancingPolicy(dCAwareRoundRobinPolicy);
        builder.withCompression(Compression.LZ4);
        for (String host : this.agentHosts) {
            builder.addContactPoints(host);
        }
        builder.withPort(9042);

        cluster = builder.build();
        session = cluster.connect(keyspace);
    }

    public void close() {
        if (null != session) {
            session.close();
        }
        if (null != cluster) {
            cluster.close();
        }
        cluster = null;
    }

    public ResultSet execute(String cql) {
        return session.execute(cql);
    }

    public Session getSession() {
        return session;
    }

    //一行记录, 返回Row
    public Row getOne(String cql, Object... paramValues) {
        ResultSet rs = session.execute(cql, paramValues);
        return rs.one();
    }

    public Row getOne(BoundStatement bstmt) {
        return session.execute(bstmt).one();
    }

    public Row getOne(PreparedStatement pstmt, Object... paramValues) {
        BoundStatement bstmt = pstmt.bind(paramValues);
        return getOne(bstmt);
    }

    //所有记录, 返回List<Row>
    public List<Row> getAll(BoundStatement bstmt) {
        ResultSet rs = session.execute(bstmt);
        return rs.all();
    }

    public List<Row> getAll(PreparedStatement pstmt, Object... paramValues) {
        BoundStatement bstmt = pstmt.bind(paramValues);
        return getAll(bstmt);
    }

    public List<Row> getAllOfSize(BoundStatement bstmt, int size) {
        List<Row> resultRows = new ArrayList<>();
        int count = 0;
        ResultSet rs = session.execute(bstmt);
        Iterator<Row> it = rs.iterator();
        while (it.hasNext() && count < size) {
            count++;
            resultRows.add(it.next());
        }
        return resultRows;
    }

    public List<Row> getAllOfSize(PreparedStatement pstmt, int count, Object... paramValues) {
        BoundStatement bstmt = pstmt.bind(paramValues);
        return getAllOfSize(bstmt, count);
    }

    //原始接口, 返回ResultSet.
    public ResultSet execute(Statement stmt) {
        return session.execute(stmt);
    }

    public ResultSet execute(PreparedStatement pstmt, Object... paramValues) {
        BoundStatement bstmt = pstmt.bind(paramValues);
        return session.execute(bstmt);
    }
}
