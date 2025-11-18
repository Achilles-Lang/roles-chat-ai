package com.blueberry.quiz_user.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blueberry.model.user.UserDTO;
import com.blueberry.model.user.UserVO;
import com.blueberry.quiz_user.entity.UserTO;
import com.blueberry.quiz_user.exception.UserException;
import com.blueberry.quiz_user.mapper.UserMapper;
import com.blueberry.quiz_user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserTO> implements UserService {

    @Override
    public boolean addUser(UserDTO userDTO) {
        var sameUsername = lambdaQuery().eq(UserTO::getUsername, userDTO.getUsername()).exists();
        if (sameUsername) {
            throw new UserException("用户名已存在");
        }
        var userTO = new UserTO();
        var encodedPassword = SecureUtil.md5(userDTO.getPassword());
        userTO.setUsername(userDTO.getUsername());
        userTO.setPassword(encodedPassword);
        return baseMapper.insert(userTO) > 0;
    }

    @Override
    public long login(UserDTO userDTO) {
        var user = lambdaQuery().eq(UserTO::getUsername, userDTO.getUsername()).list().getFirst();
        if (user == null) {
            throw new UserException("用户不存在");
        }
        var rawPassword = userDTO.getPassword();
        if (!SecureUtil.md5(rawPassword).equals(user.getPassword())) {
            throw new UserException("密码不正确");
        }
        return user.getId();
    }

    @Override
    public UserVO getUser(long userId) {
        var userTO = lambdaQuery().eq(UserTO::getId, userId).one();
        var userVO = new UserVO();
        userVO.setAvatar(userTO.getAvatar());
        userVO.setNickname(userTO.getNickname());
        return userVO;
    }
}
