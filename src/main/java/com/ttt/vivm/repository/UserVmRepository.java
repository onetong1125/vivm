package com.ttt.vivm.repository;

import com.ttt.vivm.model.UserVm;
import com.ttt.vivm.model.UserVmId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserVmRepository extends JpaRepository<UserVm, UserVmId> {
    List<UserVm> findByVm_VmId(int vmId);
}
