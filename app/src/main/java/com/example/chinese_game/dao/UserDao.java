package com.example.chinese_game.dao;

import com.example.chinese_game.javabean.User;
import java.util.List;

public interface UserDao {
    /**
     * 注册新用户
     * @param user 用户对象
     * @return 用户ID，注册失败返回-1
     */
    long register(User user);

    /**
     * 用户登录验证
     * @param username 用户名
     * @param password 密码
     * @return 用户对象，登录失败返回null
     */
    User login(String username, String password);

    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户对象，不存在返回null
     */
    User findByUsername(String username);

    /**
     * 根据ID查找用户
     * @param userId 用户ID
     * @return 用户对象，不存在返回null
     */
    User findById(int userId);

    /**
     * 更新用户信息
     * @param user 用户对象
     * @return 更新是否成功
     */
    boolean updateUser(User user);

    /**
     * 更新用户登录信息（最后登录时间和连续登录天数）
     * @param userId 用户ID
     * @return 更新是否成功
     */
    boolean updateLoginInfo(int userId);

    /**
     * 更新用户游戏统计信息
     * @param userId 用户ID
     * @param score 新增分数
     * @return 更新是否成功
     */
    boolean updateGameStats(int userId, int score);

    /**
     * 获取所有用户（用于管理员功能）
     * @return 用户列表
     */
    List<User> findAllUsers();

    /**
     * 删除用户
     * @param userId 用户ID
     * @return 删除是否成功
     */
    boolean deleteUser(int userId);

    /**
     * 从JSON文件中批量导入用户数据
     * @param jsonContent JSON字符串内容
     * @return 成功导入的用户数量，失败返回-1
     */
    int importFromJson(String jsonContent);

    /**
     * 从assets文件夹中的JSON文件导入用户数据
     * @param context Android上下文
     * @return 成功导入的用户数量，失败返回-1
     */
    int importFromAssetsJson(android.content.Context context);
}