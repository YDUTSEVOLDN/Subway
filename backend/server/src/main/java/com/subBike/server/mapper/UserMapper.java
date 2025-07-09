package com.subBike.server.mapper;

import com.subBike.server.entity.User;
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
}
