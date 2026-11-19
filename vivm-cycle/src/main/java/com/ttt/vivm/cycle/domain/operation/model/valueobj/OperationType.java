package com.ttt.vivm.cycle.domain.operation.model.valueobj;

public enum OperationType {
  START_VM("VM");

  private final String resourceType;

  OperationType(String resourceType) {
    this.resourceType = resourceType;
  }

  public String resourceType() {
    return resourceType;
  }
}
