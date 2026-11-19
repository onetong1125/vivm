package com.ttt.vivm.cycle.domain.operation.model.valueobj;

public enum OperationStatus {
  RUNNING,
  SUCCEEDED,
  FAILED,
  // 结果未知，或存在需要核对的副作用
  REQUIRES_REVIEW
}
