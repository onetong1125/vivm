package com.ttt.vivm.repository;

import com.ttt.vivm.model.VersionImg;
import com.ttt.vivm.model.VersionImgId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VersionImgRepository extends JpaRepository<VersionImg, VersionImgId> {
    List<VersionImg> findByVersion_Id(int versionId);
}
