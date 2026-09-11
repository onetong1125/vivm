package com.ttt.vivm.service;

import com.ttt.vivm.repository.UserImgRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserImageService {
    @Autowired
    UserImgRepository userImgRepository;

    public boolean existUserImage(int userId, int imageId) {
        return userImgRepository.existsByUser_UserIdAndImg_ImgId(userId, imageId);
    }
}
