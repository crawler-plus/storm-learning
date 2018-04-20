package site.it4u.bigdata;

import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;

import java.util.Map;

public class ClusterSumStormExecutorsTopology {

    /**
     * 数据源需要产生数据并发射
     */
    public static class DataSourceSpout extends BaseRichSpout {

        private SpoutOutputCollector spoutOutputCollector;

        /**
         * 初始化方法，会被调用一次
         * @param map 配置参数
         * @param topologyContext 上下文
         * @param spoutOutputCollector 数据发射器
         */
        @Override
        public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
            this.spoutOutputCollector = spoutOutputCollector;
        }

        int number = 0;

        /**
         * 会产生数据，生产上要从消息队列获取
         * 这个方法是个死循环
         */
        @Override
        public void nextTuple() {
            this.spoutOutputCollector.emit(new Values(++number));
            Utils.sleep(1000);
            System.out.println("spout:" + number);
        }

        /**
         * 声明输出字段
         * @param outputFieldsDeclarer
         */
        @Override
        public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
            outputFieldsDeclarer.declare(new Fields("num"));
        }
    }

    /**
     * 累计求和bolt，接收数据并处理
     */
    public static class SumBolt extends BaseRichBolt {

        /**
         * 初始化方法，被执行一次
         * @param map
         * @param topologyContext
         * @param outputCollector
         */
        @Override
        public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        }

        int sum = 0;

        /**
         * 也是一个死循环，获取spout发过来的数据
         * @param tuple
         */
        @Override
        public void execute(Tuple tuple) {
            Integer num = tuple.getIntegerByField("num");
            sum += num;
            System.out.println("sum=" + sum);
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        }
    }

    public static void main(String[] args) {
        // 根据spout和bolt构建出topology
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("DataSourceSpout", new DataSourceSpout(), 2); // spout设置2个executor
        builder.setBolt("SumBolt", new SumBolt(), 2).shuffleGrouping("DataSourceSpout"); // bolt设置2个executor
        String topoName = ClusterSumStormExecutorsTopology.class.getSimpleName();
        // 代码提交到storm集群上
        try {
            Config config = new Config();
            config.setNumWorkers(2); // 设置2个worker
            config.setNumAckers(0); // 不需要ACK(生产上不要设置，否则不能确保不重复消费）
            StormSubmitter.submitTopology(topoName, config, builder.createTopology());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

