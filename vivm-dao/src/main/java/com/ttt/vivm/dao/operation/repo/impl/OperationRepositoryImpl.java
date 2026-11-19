package com.ttt.vivm.dao.operation.repo.impl;

import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;
import com.ttt.vivm.cycle.domain.operation.repo.OperationRepository;
import com.ttt.vivm.dao.operation.assembler.OperationAssembler;
import com.ttt.vivm.dao.operation.mapper.OperationMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class OperationRepositoryImpl implements OperationRepository {
  private final OperationMapper operationMapper;

  public OperationRepositoryImpl(OperationMapper operationMapper) {
    this.operationMapper = operationMapper;
  }

  @Override
  public void create(OperationE operation) {
    operationMapper.insert(OperationAssembler.toRecord(operation));
  }

  @Override
  public Optional<OperationE> findById(String id) {
    return Optional.ofNullable(operationMapper.selectById(id))
        .map(OperationAssembler::toEntity);
  }
}
