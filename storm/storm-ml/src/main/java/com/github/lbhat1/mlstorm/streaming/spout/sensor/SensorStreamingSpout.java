package com.github.lbhat1.mlstorm.streaming.spout.sensor;

import backtype.storm.Config;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import com.github.lbhat1.mlstorm.streaming.spout.utils.MsSqlServerSensorDbUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: lbhat <laksh85@gmail.com>
 * Date: 12/18/13
 * Time: 6:53 PM
 */
public class SensorStreamingSpout implements IRichSpout {
    private final String[] fields;
    private Connection jdbcConnection;
    private ResultSet sensor;
    private SpoutOutputCollector collector;

    public SensorStreamingSpout(final String[] fields) {
        this.fields = fields;
    }

    /**
     * Method name is self explanatory
     *
     * @throws SQLException
     */
    private void moveSpoutForward() throws SQLException {
        if (!this.sensor.next()) {
            this.sensor = null;
        }
    }

    /**
     * Declare the output schema for all the streams of this topology.
     *
     * @param declarer this is used to declare output stream ids, output fields, and whether or not each output stream is a direct stream
     */
    @Override
    public void declareOutputFields(final OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(fields));
    }

    /**
     * Declare configuration specific to this component. Only a subset of the "topology.*" configs can
     * be overridden. The component configuration can be further overridden when constructing the
     * topology using {@link backtype.storm.topology.TopologyBuilder}
     */
    @Override
    public Map<String, Object> getComponentConfiguration() {
        return new Config();
    }

    /**
     * Called when a task for this component is initialized within a worker on the cluster.
     * It provides the spout with the environment in which the spout executes.
     * <p/>
     * <p>This includes the:</p>
     *
     * @param conf      The Storm configuration for this spout. This is the configuration provided to the topology merged in with cluster configuration on this machine.
     * @param context   This object can be used to get information about this task's place within the topology, including the task id and component id of this task, input and output information, etc.
     * @param collector The collector is used to emit tuples from this spout. Tuples can be emitted at any time, including the open and close methods. The collector is thread-safe and should be saved as an instance variable of this spout object.
     */
    @Override
    public void open(final Map conf, final TopologyContext context, final SpoutOutputCollector collector) {
        try {
            Logger.getAnonymousLogger().log(Level.INFO, MessageFormat.format("Starting JHU sensor spout. Connecting to database at {0}", MsSqlServerSensorDbUtils.DB_URL));
            jdbcConnection = MsSqlServerSensorDbUtils.getNewDatabaseConnection(MsSqlServerSensorDbUtils.DB_USER, MsSqlServerSensorDbUtils.DB_USER);
            sensor = MsSqlServerSensorDbUtils.getAllFromSensorDb(jdbcConnection, MsSqlServerSensorDbUtils.TABLE_NAME, MsSqlServerSensorDbUtils.ORDER_BY_COLUMN);
            moveSpoutForward();
            this.collector = collector;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Called when an ISpout is going to be shutdown. There is no guarantee that close
     * will be called, because the supervisor kill -9's worker processes on the cluster.
     * <p/>
     * <p>The one context where close is guaranteed to be called is a topology is
     * killed when running Storm in local mode.</p>
     */
    @Override
    public void close() {
        try {
            sensor.close();
            jdbcConnection.close();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Called when a spout has been activated out of a deactivated mode.
     * nextTuple will be called on this spout soon. A spout can become activated
     * after having been deactivated when the topology is manipulated using the
     * `storm` client.
     */
    @Override
    public void activate() {
        try {
            if (!jdbcConnection.isValid(2)) {
                jdbcConnection.close();
                jdbcConnection = MsSqlServerSensorDbUtils.getNewDatabaseConnection(MsSqlServerSensorDbUtils.DB_USER, MsSqlServerSensorDbUtils.DB_USER);
                sensor = MsSqlServerSensorDbUtils.getAllFromSensorDb(jdbcConnection, MsSqlServerSensorDbUtils.TABLE_NAME, MsSqlServerSensorDbUtils.ORDER_BY_COLUMN);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Called when a spout has been deactivated. nextTuple will not be called while
     * a spout is deactivated. The spout may or may not be reactivated in the future.
     */
    @Override
    public void deactivate() {
    }

    /**
     * When this method is called, Storm is requesting that the Spout emit tuples to the
     * output collector. This method should be non-blocking, so if the Spout has no tuples
     * to emit, this method should return. nextTuple, ack, and fail are all called in a tight
     * loop in a single thread in the spout task. When there are no tuples to emit, it is courteous
     * to have nextTuple sleep for a short amount of time (like a single millisecond)
     * so as not to waste too much CPU.
     */
    @Override
    public void nextTuple() {
        try {
            if (sensor != null) {
                String sensorName = sensor.getString(MsSqlServerSensorDbUtils.SENSOR_COLUMN);
                double temperature = sensor.getDouble(MsSqlServerSensorDbUtils.DATA_COLUMN);
                collector.emit(new Values(sensorName, temperature));
                // todo (fix this) this is a hack.
                // we need the following if we are going to wrap this spout in a batch spout
                moveSpoutForward();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Storm has determined that the tuple emitted by this spout with the msgId identifier
     * has been fully processed. Typically, an implementation of this method will take that
     * message off the queue and prevent it from being replayed.
     *
     * @param msgId
     */
    @Override
    public void ack(final Object msgId) {
        try {
            moveSpoutForward();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The tuple emitted by this spout with the msgId identifier has failed to be
     * fully processed. Typically, an implementation of this method will put that
     * message back on the queue to be replayed at a later time.
     *
     * @param msgId
     */
    @Override
    public void fail(final Object msgId) {
        // do nothing
    }
}
