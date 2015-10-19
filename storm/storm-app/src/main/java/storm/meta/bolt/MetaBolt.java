package storm.meta.bolt;

import java.util.Map;
import com.taobao.metamorphosis.client.MetaClientConfig;
import com.taobao.metamorphosis.exception.MetaClientException;
import com.taobao.metamorphosis.utils.ZkUtils.ZKConfig;
import com.taobao.metamorphosis.Message;
import com.taobao.metamorphosis.client.MessageSessionFactory;
import com.taobao.metamorphosis.client.MetaMessageSessionFactory;
import com.taobao.metamorphosis.client.producer.MessageProducer;
import com.taobao.metamorphosis.client.producer.SendResult;
import storm.meta.base.ConfCheck;
import storm.meta.base.MacroDef;
import storm.meta.xml.MetaXml;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.IRichBolt;
import backtype.storm.tuple.Tuple;

/**
 * @author blogchong
 * @Blog www.blogchong.com
 * @email blogchong@gmail.com
 * @QQ_G 191321336
 * @version 2014年11月16日 上午20:33:22
 */

@SuppressWarnings("serial")
public class MetaBolt implements IRichBolt {

	private MetaClientConfig metaClientConfig;

	// 是否加载配置标志位
	private static boolean flag_load = false;

	private transient MessageSessionFactory sessionFactory;

	private transient MessageProducer messageProducer;

	public static String Topic;

	private transient SendResult sendResult;

	String metaXml = "MetaBolt.xml";

	private boolean flag_par = true;

	private long meta_debug = 10000;
	private long register = 0;
	private long reg_tmp = 0;

	public MetaBolt(String MetaXml) {
		super();

		if (MetaXml == null) {
			flag_par = false;
		} else {
			this.metaXml = MetaXml;
		}

	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public void prepare(final Map conf, TopologyContext context,
			final OutputCollector collector) {
		System.out.println("MetaBolt	--	Start!");

		this.reg_tmp = MacroDef.meta_debug;

		if (this.flag_par == false) {
			System.out
					.println("MetaSpout-- Erre: can't get the path of Spout.xml!");
		} else {
			// 调用检测线程
			new ConfCheck(this.metaXml, MacroDef.HEART_BEAT,
					MacroDef.Thread_type_monitorbolt).start();
		}

	}

	@SuppressWarnings({ "static-access", "unused" })
	@Override
	public void execute(Tuple input) {
		String str = input.getString(0);

		if (this.flag_par == false) {
			System.out
					.println("MeataBolt-- Erre: can't get the path of MetaBolt.xml!");
		} else {

			try {

				// 检测配置文件是否更改
				if (flag_load == false) {
					// 配置文件发生更改则进行加载参数操作
					Loading();
					if (register != 0) {
						System.out.println("MetaBolt-- Conf Change: "
								+ this.metaXml);
					} else {
						System.out.println("MetaBolt-- Conf Loaded: "
								+ this.metaXml);
					}
				}

				if (str != null) {
					// 写入metaq的数据必须加上换行
					str = str + MacroDef.FLAG_ROW;
					this.sendResult = this.messageProducer
							.sendMessage(new Message(this.Topic, str.getBytes()));// 往metaq写数据

					if (!sendResult.isSuccess()) {
						System.err
								.println("MetaBolt-- Send message failed,error message:"
										+ sendResult.getErrorMessage());
						System.err.println("MetaBolt-- Error Tuple: " + str);
					} else {

						this.register++;
						if (this.register >= this.reg_tmp) {
							if (MacroDef.meta_flag == true) {
								System.out
										.println("MetaBolt	--	Send Tuple Count: "
												+ this.register);
							}
							this.reg_tmp = this.register + this.meta_debug;
						}// 数量统计

					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// 更改标志位
	public static void isload() {
		flag_load = false;
	}

	// 加载参数操作
	@SuppressWarnings("static-access")
	public void Loading() {

		new MetaXml(this.metaXml).read();
		// MetaSpout参数
		this.Topic = MetaXml.MetaTopic; // 读取接收Topic

		// 获取连接zk配置(metaq)
		ZKConfig zkconf = new ZKConfig();
		// zkconf.zkConnect
		zkconf.zkConnect = MetaXml.MetaZkConnect;
		// zkconf.zkRoot = "/meta";
		zkconf.zkRoot = MetaXml.MetaZkRoot;
		MetaClientConfig metaconf = new MetaClientConfig();
		metaconf.setZkConfig(zkconf);

		this.metaClientConfig = metaconf;

		if (this.Topic == null) {
			throw new IllegalArgumentException(this.Topic + ":" + " is null");
		}

		try {

			this.sessionFactory = new MetaMessageSessionFactory(
					this.metaClientConfig);
			this.messageProducer = this.sessionFactory.createProducer();
			this.messageProducer.publish(this.Topic);

		} catch (final MetaClientException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void cleanup() {
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
	}

	@Override
	public Map<String, Object> getComponentConfiguration() {
		return null;
	}

}
