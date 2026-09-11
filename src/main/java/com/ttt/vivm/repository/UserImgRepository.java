package com.ttt.vivm.repository;

import com.ttt.vivm.model.UserImg;
import com.ttt.vivm.model.UserImgId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserImgRepository extends JpaRepository<UserImg, UserImgId> {
    boolean existsByUser_UserIdAndImg_ImgId(int userId, int imgId);
}
