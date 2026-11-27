package com.ttt.vivm.dao.operation.record;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("operation_task")
public class OperationRecord {
  @TableId(type = IdType.INPUT)
  public String id;
  public Integer actorId;
  public Integer poolId;
  public String operationType;
  public String resourceType;
  public String resourceId;
  public String status;
  public String step;
  public LocalDateTime createdAt;
  public LocalDateTime startedAt;
  public LocalDateTime finishedAt;
  public long version;
}
