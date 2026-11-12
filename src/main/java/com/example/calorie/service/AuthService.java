package com.example.calorie.service;

import com.example.calorie.dto.LoginRequest;
import com.example.calorie.dto.RegisterRequest;
import com.example.calorie.vo.LoginVO;
import com.example.calorie.vo.UserVO;

public interface AuthService {

    UserVO register(RegisterRequest request);

    LoginVO login(LoginRequest request);

    void logout();

    UserVO currentUser();
}
