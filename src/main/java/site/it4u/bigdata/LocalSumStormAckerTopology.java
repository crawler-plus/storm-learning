package site.it4u.bigdata;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
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

public class LocalSumStormAckerTopology {

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
            ++number;
            // 发射出去的两个参数，第一个是数据，第二个是编号，如果是数据库，msgId可以采用主键
            this.spoutOutputCollector.emit(new Values(number), number);
            Utils.sleep(1000);
            System.out.println("spout:" + number);
        }

        @Override
        public void ack(Object msgId) {
            System.out.println("ack invoked..." + msgId);
        }

        /**
         * 在此处对失败的数据重发或者保存
         * @param msgId
         */
        @Override
        public void fail(Object msgId) {
            System.out.println("fail invoked..." + msgId);
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

        private OutputCollector outputCollector;

        /**
         * 初始化方法，被执行一次
         * @param map
         * @param topologyContext
         * @param outputCollector
         */
        @Override
        public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
            this.outputCollector = outputCollector;
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
            // 工作中用try catch判断
            if(num > 0 && num <= 10) {
                this.outputCollector.ack(tuple); // 确认消息处理成功
            }
            // 假设大于10就是失败的
            else {
                this.outputCollector.fail(tuple); // 确认消息处理失败
            }
            System.out.println("sum=" + sum);
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        }
    }

    public static void main(String[] args) {
        // 根据spout和bolt构建出topology
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("DataSourceSpout", new DataSourceSpout());
        builder.setBolt("SumBolt", new SumBolt()).shuffleGrouping("DataSourceSpout");
        // 创建一个本地模式运行的storm集群
        LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("LocalSumStormTopology", new Config(), builder.createTopology());
    }
}

