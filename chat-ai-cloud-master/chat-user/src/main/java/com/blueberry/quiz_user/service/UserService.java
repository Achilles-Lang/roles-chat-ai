package com.blueberry.quiz_user.service;

import com.achilles.model.user.UserDTO;
import com.achilles.model.user.UserVO;

public interface UserService {
    boolean addUser(UserDTO userDTO);
    long login(UserDTO userDTO);
    UserVO getUser(long userId);
}
