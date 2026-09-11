package com.ttt.vivm.repository;

import com.ttt.vivm.model.Vm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VmRepository extends JpaRepository<Vm, Integer> {
    Optional<Vm> findByVmName(String vmName);
    List<Vm> findByVmNameContaining(String name);
    boolean existsByVmName(String vmName);
    List<Vm> findByVmType(int vmType);
    List<Vm> findByVmStatusNot(int vmStatus);
}
