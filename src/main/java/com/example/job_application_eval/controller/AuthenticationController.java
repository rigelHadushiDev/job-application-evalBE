package com.example.job_application_eval.controller;


import com.example.job_application_eval.dtos.LoginUserDto;
import com.example.job_application_eval.dtos.RegisterUserDto;
import com.example.job_application_eval.dtos.UserDto;
import com.example.job_application_eval.entities.UserEntity;
import com.example.job_application_eval.mappers.Mapper;
import com.example.job_application_eval.responses.LogInResponse;
import com.example.job_application_eval.service.AuthenticationService;
import com.example.job_application_eval.service.JwtService;
import com.example.job_application_eval.service.impl.JwtServiceImpl;
import com.example.job_application_eval.service.impl.AuthenticationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/auth")
@RestController
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final Mapper<UserEntity, UserDto> userMapper;

    @PostMapping("/signup")
    public ResponseEntity<UserDto> register(@RequestBody UserDto userDto) {
        UserEntity userEntity = userMapper.mapFrom(userDto);
        UserEntity registeredUser = authenticationService.signup(userEntity);
        return new ResponseEntity<>( userMapper.mapTo(registeredUser), HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<LogInResponse> authenticate(@RequestBody LoginUserDto loginUserDto) {
        LogInResponse response = authenticationService.authenticate(loginUserDto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendAutoGeneratedPassword(@RequestParam String email) {
            authenticationService.resendAutoGeneratedPassword(email);
            return ResponseEntity.ok("Verification code sent");
    }
}