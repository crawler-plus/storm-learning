package site.it4u.bigdata.integration.redis;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.redis.bolt.RedisStoreBolt;
import org.apache.storm.redis.common.config.JedisPoolConfig;
import org.apache.storm.redis.common.mapper.RedisDataTypeDescription;
import org.apache.storm.redis.common.mapper.RedisStoreMapper;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.ITuple;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 词频统计
 */
public class LocalWordCountRedisStormTopology {

    public static class DataSourceSpout extends BaseRichSpout {

        private SpoutOutputCollector spoutOutputCollector;

        @Override
        public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
            this.spoutOutputCollector = spoutOutputCollector;
        }

        public static final String[] words = new String[]{"apple", "orange", "banana"};

        @Override
        public void nextTuple() {
            Random random = new Random();
            String word = words[random.nextInt(words.length)];
            this.spoutOutputCollector.emit(new Values(word));
            System.out.println("emit:" + word);
            Utils.sleep(1000);
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
            outputFieldsDeclarer.declare(new Fields("line"));
        }
    }

    /**
     * 分割数据
     */
    public static class SplitBolt extends BaseRichBolt {

        private OutputCollector outputCollector;

        @Override
        public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
            this.outputCollector = outputCollector;
        }

        /**
         * 对line按照逗号分割
         * @param tuple
         */
        @Override
        public void execute(Tuple tuple) {
            String word = tuple.getStringByField("line");
            this.outputCollector.emit(new Values(word));
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
            outputFieldsDeclarer.declare(new Fields("word"));
        }
    }

    /**
     * 词频汇总bolt
     */
    public static class CountBolt extends BaseRichBolt {

        private OutputCollector outputCollector;

        @Override
        public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
            this.outputCollector = outputCollector;
        }

        Map<String, Integer> map = new HashMap<>();

        /**
         * 获取每个单词
         * 对所有单词汇总
         * 输出
         * @param tuple
         */
        @Override
        public void execute(Tuple tuple) {
            String word = tuple.getStringByField("word");
            Integer count = map.get(word);
            if(count == null) {
                count = 0;
            }
            count ++;
            // 放数据
            map.put(word, count);
            this.outputCollector.emit(new Values(word, map.get(word)));
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
            outputFieldsDeclarer.declare(new Fields("word", "count"));
        }
    }

    /**
     * 存入redis
     */
    public static class WordCountStoreMapper implements RedisStoreMapper {

        private RedisDataTypeDescription description;

        private final String hashKey = "wc";

        public WordCountStoreMapper() {
            description = new RedisDataTypeDescription(
                    RedisDataTypeDescription.RedisDataType.HASH, hashKey);
        }

        public RedisDataTypeDescription getDataTypeDescription() {
            return description;
        }

        public String getKeyFromTuple(ITuple tuple) {
            return tuple.getStringByField("word");
        }

        public String getValueFromTuple(ITuple tuple) {
            return tuple.getIntegerByField("count") + "";
        }
    }

    public static void main(String[] args) {
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("DataSourceSpout", new DataSourceSpout());
        builder.setBolt("SplitBolt", new SplitBolt()).shuffleGrouping("DataSourceSpout");
        builder.setBolt("CountBolt", new CountBolt()).shuffleGrouping("SplitBolt");

        JedisPoolConfig poolConfig = new JedisPoolConfig.Builder()
                .setHost("10.34.1.161").setPort(6379).build();
        RedisStoreMapper storeMapper = new WordCountStoreMapper();
        RedisStoreBolt storeBolt = new RedisStoreBolt(poolConfig, storeMapper);
        builder.setBolt("RedisStoreBolt", storeBolt).shuffleGrouping("CountBolt");
        LocalCluster localCluster = new LocalCluster();
        localCluster.submitTopology("LocalWordCountRedisStormTopology", new Config(), builder.createTopology());
    }
}
