package com.subBike.server.service;

import com.subBike.server.entity.dto.UserDto;

public interface IUserService {
    /**
     * insert a user
     * @param uesr
     */
    void add(UserDto uesr);
}

