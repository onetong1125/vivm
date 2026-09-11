package com.ttt.vivm.repository;

import com.ttt.vivm.model.Img;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImgRepository extends JpaRepository<Img, Integer> {
    boolean existsByImgName(String imgName);
    Optional<Img> findByImgName(String imgName);
    List<Img> findByImgNameContaining(String name);
}
