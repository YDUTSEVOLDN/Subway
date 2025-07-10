package com.subBike.server.mapper;

import com.subBike.server.entity.User;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository //spring bean
public interface UserMapper extends CrudRepository<User,Integer> {
    // 根据用户名查找用户
    Optional<User> findByUserName(String userName);
    //根据ID查找用户
    Optional<User> findByUserID(Integer userID);
    // 根据邮箱查找用户
    Optional<User> findByEmail(String email);

    // 检查用户名或邮箱是否存在
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);

    @Query("Update User "+
            "SET userName=:new_name,password=:new_password,email=:new_email "+
            "WHERE userName=:old_name")
    void editUser(@Param("old_name") String old_name,@Param("new_name") String new_name,@Param("new_password") String new_password,@Param("new_email") String new_email);

    @Query("DELETE User "+
            "WHERE userName=:name")
    void deleteUser(@Param("name") String name);

    // 新增：根据用户名和密码查找用户（用于登录验证）
    @Query("SELECT u FROM User u WHERE u.userName = :userName AND u.password = :password")
    Optional<User> findByUserNameAndPassword(@Param("userName") String userName,
                                             @Param("password") String password);
}
