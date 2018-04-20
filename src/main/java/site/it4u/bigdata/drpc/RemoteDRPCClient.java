package site.it4u.bigdata.drpc;

import org.apache.storm.Config;
import org.apache.storm.utils.DRPCClient;

/**
 * Remote DRPC客户端测试类
 */
public class RemoteDRPCClient {

    public static void main(String[] args) throws Exception {
        Config config = new Config();
        // 一组参数
        config.put("storm.thrift.transport", "org.apache.storm.security.auth.SimpleTransportPlugin");
        config.put(Config.STORM_NIMBUS_RETRY_TIMES, 3);
        config.put(Config.STORM_NIMBUS_RETRY_INTERVAL, 10);
        config.put(Config.STORM_NIMBUS_RETRY_INTERVAL_CEILING, 20);
        config.put(Config.DRPC_MAX_BUFFER_SIZE, 1048576);
        DRPCClient client = new DRPCClient(config,"host", 3772);
        String result = client.execute("addUser", "lisi");
        System.out.println("Client invoked: " + result);
    }
}
