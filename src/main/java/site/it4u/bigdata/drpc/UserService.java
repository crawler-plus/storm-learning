package site.it4u.bigdata.drpc;

/**
 * 用户服务
 */
public interface UserService {

    long versionID = 88888888L;

    /**
     * 添加用户
     * @param name 名字
     * @param age 年龄
     */
    public void addUser(String name, int age);
}
