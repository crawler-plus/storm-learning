package site.it4u.bigdata;

import org.apache.commons.io.FileUtils;
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

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 词频统计
 */
public class LocalWordCountStormTopology {

    public static class DataSourceSpout extends BaseRichSpout {

        private SpoutOutputCollector spoutOutputCollector;

        @Override
        public void open(Map map, TopologyContext topologyContext, SpoutOutputCollector spoutOutputCollector) {
            this.spoutOutputCollector = spoutOutputCollector;
        }

        @Override
        public void nextTuple() {
            Collection<File> files = FileUtils.listFiles(new File("wc"), new String[]{"txt"}, true);
            for (File file : files) {
                try {
                    List<String> lines = FileUtils.readLines(file);
                    for (String line : lines) {
                        this.spoutOutputCollector.emit(new Values(line));
                    }
                    // 数据处理完之后改名，否则重复执行
                    FileUtils.moveFile(file, new File(file.getAbsolutePath() + System.currentTimeMillis()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
            String line = tuple.getStringByField("line");
            String[] words = line.split(",");
            for (String word : words) {
                this.outputCollector.emit(new Values(word));
            }
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
        @Override
        public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
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
            Set<Map.Entry<String, Integer>> entries = map.entrySet();
            for (Map.Entry<String, Integer> entry : entries) {
                System.out.println(entry);
            }
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        }
    }

    public static void main(String[] args) {
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("DataSourceSpout", new DataSourceSpout());
        builder.setBolt("SplitBolt", new SplitBolt()).shuffleGrouping("DataSourceSpout");
        builder.setBolt("CountBolt", new CountBolt()).shuffleGrouping("SplitBolt");
        LocalCluster localCluster = new LocalCluster();
        localCluster.submitTopology("LocalWordCountStormTopology", new Config(), builder.createTopology());
    }
}
