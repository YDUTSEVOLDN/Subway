package com.subBike.server.mapper;

import com.subBike.server.entity.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository //spring bean
public interface UserMapper extends CrudRepository<User,Integer> {
}
