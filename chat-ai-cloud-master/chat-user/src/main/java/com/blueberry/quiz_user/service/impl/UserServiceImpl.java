package com.blueberry.quiz_user.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.achilles.model.user.UserDTO;
import com.achilles.model.user.UserVO;
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
        // 修改点：使用 .one() 代替 list().getFirst()
        // .one() 会直接去数据库查一条记录，如果没有就是 null，写法更标准兼容性更好
        UserTO user = lambdaQuery().eq(UserTO::getUsername, userDTO.getUsername()).one();

        if (user == null) {
            throw new UserException("用户不存在");
        }

        var rawPassword = userDTO.getPassword();
        // 对比密码（注意：这里 user.getPassword() 现在能用了，因为第一步修好了）
        if (!SecureUtil.md5(rawPassword).equals(user.getPassword())) {
            throw new UserException("密码不正确");
        }
        return user.getId();
    }

    @Override
    public UserVO getUser(long userId) {
        var userTO = lambdaQuery().eq(UserTO::getId, userId).one();
        if (userTO == null) {
            return null;
        }
        var userVO = new UserVO();
        userVO.setAvatar(userTO.getAvatar());
        userVO.setNickname(userTO.getNickname());
        return userVO;
    }
}