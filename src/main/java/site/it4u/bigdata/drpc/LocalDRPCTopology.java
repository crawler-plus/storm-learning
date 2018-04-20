package site.it4u.bigdata.drpc;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.LocalDRPC;
import org.apache.storm.drpc.LinearDRPCTopologyBuilder;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.util.Map;

/**
 * 本地的DRPC
 */
public class LocalDRPCTopology {

    public static class MyBolt extends BaseRichBolt {

        private OutputCollector outputCollector;

        @Override
        public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
            this.outputCollector = outputCollector;
        }

        @Override
        public void execute(Tuple tuple) {
            Object requestId = tuple.getValue(0); // 请求的id
            String name = tuple.getString(1); // 请求的参数
            String result = "add user:" + name;
            this.outputCollector.emit(new Values(requestId, result));
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
            outputFieldsDeclarer.declare(new Fields("id", "result"));
        }
    }

    public static void main(String[] args) {
        LinearDRPCTopologyBuilder builder = new LinearDRPCTopologyBuilder("addUser");
        builder.addBolt(new MyBolt());
        LocalCluster localCluster = new LocalCluster();
        LocalDRPC drpc = new LocalDRPC();
        localCluster.submitTopology("local-drpc", new Config(), builder.createLocalTopology(drpc));
        String result = drpc.execute("addUser", "zhangsan");
        System.out.println("from client:" + result);
        localCluster.shutdown();
        drpc.shutdown();
    }
}
