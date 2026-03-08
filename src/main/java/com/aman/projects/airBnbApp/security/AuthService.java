package com.aman.projects.airBnbApp.security;

import com.aman.projects.airBnbApp.dto.LoginDto;
import com.aman.projects.airBnbApp.dto.SignUpRequestDto;
import com.aman.projects.airBnbApp.dto.UserDto;
import com.aman.projects.airBnbApp.entity.User;
import com.aman.projects.airBnbApp.entity.enums.Role;
import com.aman.projects.airBnbApp.exceptions.ResourceNotFoundException;
import com.aman.projects.airBnbApp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtServices jwtServices;
    public UserDto signUp(SignUpRequestDto signUpRequestDto){
     User user=userRepository.findByEmail(signUpRequestDto.getEmail()).orElse(null);
     if(user!=null){
         throw new RuntimeException("User is already present with same email id");
     }
     User newUser=modelMapper.map(signUpRequestDto, User.class);
     newUser.setRoleSet(Set.of(Role.GUEST));
     newUser.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));
     newUser=userRepository.save(newUser);
     return modelMapper.map(newUser,UserDto.class);
    }
    public String[] login(LoginDto loginDto){
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDto.getEmail(), loginDto.getPassword()
        ));
        User user=(User) authenticate.getPrincipal();
        String arr[]=new String[2];
        arr[0]=jwtServices.generateAccessToken(user);
        arr[1]=jwtServices.generateAccessToken(user);
        return arr;
    }
    public String refreshToken(String refreshToken){
        Long id=jwtServices.getUserIdFromToken(refreshToken);
        User user=userRepository.findById(id).orElseThrow(()->
                new ResourceNotFoundException("User not found with id: "+id));
        return jwtServices.generateAccessToken(user);
    }
}
