package site.it4u.bigdata.drpc;

/**
 * 用户服务接口实现
 */
public class UserServiceImpl implements UserService {

    @Override
    public void addUser(String name, int age) {
        System.out.println("Server invoked add user success, name is " + name);
    }
}
