package com.ttt.vivm.repository;

import com.ttt.vivm.model.ImgVm;
import com.ttt.vivm.model.ImgVmId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImgVmRepository extends JpaRepository<ImgVm, ImgVmId> {
    Optional<ImgVm> findByVm_VmId(int vmId);
    List<ImgVm> findByImg_ImgId(int imgId);
    boolean existsByImg_ImgId(int imgId);
}
