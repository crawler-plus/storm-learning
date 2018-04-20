package site.it4u.bigdata.integration.kafka;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.kafka.*;
import org.apache.storm.spout.SchemeAsMultiScheme;
import org.apache.storm.topology.TopologyBuilder;

import java.util.UUID;

/**
 * kafka整合storm测试
 */
public class StormKafkaTopology {

    public static void main(String[] args) {
        TopologyBuilder topologyBuilder = new TopologyBuilder();
        // kafka使用的zk地址
        BrokerHosts hosts = new ZkHosts("10.34.1.161:2181");
        // kafka存储数据的topic名称
        String topicName = "project_topic";
        // 指定zk中的一个根目录，存储的是kafkaSpout读取数据的位置信息（offset）
        String zkRoot = "/" + topicName;
        SpoutConfig spoutConfig = new SpoutConfig(hosts, topicName, zkRoot, UUID.randomUUID().toString());
        spoutConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
        KafkaSpout kafkaSpout = new KafkaSpout(spoutConfig);
        String spoutId = KafkaSpout.class.getSimpleName();
        topologyBuilder.setSpout(spoutId, kafkaSpout);
        String boltId = LogProcessBolt.class.getSimpleName();
        topologyBuilder.setBolt(boltId, new LogProcessBolt()).shuffleGrouping(spoutId);
        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology(StormKafkaTopology.class.getSimpleName(), new Config(), topologyBuilder.createTopology());
    }
}
