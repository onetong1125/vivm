package com.ttt.vivm.repository;

import com.ttt.vivm.model.ImgPool;
import com.ttt.vivm.model.ImgPoolId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImgPoolRepository extends JpaRepository<ImgPool, ImgPoolId> {
    List<ImgPool> findByPool_Id(Integer poolId);
}
