package com.blueberry.quiz_user.service;

import com.blueberry.model.user.UserDTO;
import com.blueberry.model.user.UserVO;

public interface UserService {
    boolean addUser(UserDTO userDTO);
    long login(UserDTO userDTO);
    UserVO getUser(long userId);
}
