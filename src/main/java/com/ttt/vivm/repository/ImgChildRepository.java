package com.ttt.vivm.repository;

import com.ttt.vivm.model.ImgChild;
import com.ttt.vivm.model.ImgChildId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImgChildRepository extends JpaRepository<ImgChild, ImgChildId> {
    boolean existsByParentImg_ImgId(int imgId);
    List<ImgChild> findByParentImg_ImgId(int imgId);
}
