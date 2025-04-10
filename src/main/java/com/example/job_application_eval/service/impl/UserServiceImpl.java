package com.example.job_application_eval.service.impl;

import com.example.job_application_eval.dtos.ChangePasswordDto;
import com.example.job_application_eval.entities.UserEntity;
import com.example.job_application_eval.repository.UserRepository;
import com.example.job_application_eval.responses.GeneralSuccessfulResp;
import com.example.job_application_eval.service.AuthenticationService;
import com.example.job_application_eval.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authService;

    @Override
    public List<UserEntity> allUsers() {
        return userRepository.findAll();
    }

    @Override
    public UserEntity getCurrentUserEntity() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "userNotFound"));
    }

    @Override
    public GeneralSuccessfulResp changeUserPassword(ChangePasswordDto changePasswordDto) {
        UserEntity user = getCurrentUserEntity();
        user.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
        user.setPasswordChanged(true);
        user.setAutoGeneratedPasswordExpiresAt(null);
        userRepository.save(user);
        return new GeneralSuccessfulResp("successfullyChangedPassword");
    }


    @Override
    public UserEntity deleteYourUserAccount() {
        UserEntity currentUser = getCurrentUserEntity();
        userRepository.delete(currentUser);
        return currentUser;
    }

    @Override
    public UserEntity deleteUser(Long userId) {

        UserEntity currentUser = userRepository.findUserById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "userNotFound"));
        userRepository.delete(currentUser);
        return currentUser;
    }

    @Override
    public UserEntity editCurrUserData(UserEntity userEntity) {

        UserEntity currentUser = getCurrentUserEntity();

        AtomicBoolean fullNameChanged = new AtomicBoolean(false);

        Optional.ofNullable(userEntity.getFirstname()).ifPresent(newFirstName -> {
            if (!newFirstName.equals(currentUser.getFirstname())) {
                currentUser.setFirstname(newFirstName);
                fullNameChanged.set(true);
            }
        });

        Optional.ofNullable(userEntity.getLastname()).ifPresent(newLastName -> {
            if (!newLastName.equals(currentUser.getLastname())) {
                currentUser.setLastname(newLastName);
                fullNameChanged.set(true);
            }
        });

        if (fullNameChanged.get()) {
            String updatedFirst = Optional.ofNullable(currentUser.getFirstname()).orElse("");
            String updatedLast = Optional.ofNullable(currentUser.getLastname()).orElse("");
            currentUser.setFullName((updatedFirst + " " + updatedLast).trim());
        }

        Optional.ofNullable(userEntity.getGender()).ifPresent(currentUser::setGender);
        Optional.ofNullable(userEntity.getBirthdate()).ifPresent(currentUser::setBirthdate);
        Optional.ofNullable(userEntity.getAddress()).ifPresent(currentUser::setAddress);
        Optional.ofNullable(userEntity.getMobileNumber()).ifPresent(currentUser::setMobileNumber);
        Optional.ofNullable(userEntity.getFullName()).ifPresent(currentUser::setFullName);

        return userRepository.save(currentUser);
    }


    @Override
    public Page<UserEntity> searchUsersByFullName(String fullName, Pageable pageable) {
        return userRepository.findByFullNameContainingIgnoreCase(fullName, pageable);
    }


    @Override
    public UserEntity save(UserEntity userEntity) {
        String rawTemporaryPassword = authService.generateTemporaryPassword();
        String hashedTemporaryPassword = passwordEncoder.encode(rawTemporaryPassword);

        UserEntity user = UserEntity.builder()
                .email(userEntity.getEmail())
                .password(hashedTemporaryPassword)
                .lastname(userEntity.getLastname())
                .firstname(userEntity.getFirstname())
                .gender(userEntity.getGender())
                .birthdate(userEntity.getBirthdate())
                .fullName(userEntity.getFirstname() + " " + userEntity.getLastname())
                .role(userEntity.getRole())
                .autoGeneratedPasswordExpiresAt(LocalDateTime.now().plusHours(2))
                .passwordChanged(false)
                .build();

        authService.sendVerificationEmail(user, rawTemporaryPassword);
        return userRepository.save(user);
    }
}
