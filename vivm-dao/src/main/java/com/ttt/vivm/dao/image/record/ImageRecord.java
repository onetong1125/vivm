package com.ttt.vivm.dao.image.record;

import java.time.LocalDateTime;

public class ImageRecord {
    public Integer id;
    public String name;
    public Long size;
    public String backingImagePath;
    public String imagePath;
    public Integer imageType;
    public Integer deleted;
    public LocalDateTime createDate;
    public LocalDateTime deleteDate;
    public String osType;
    public String description;
    public String tag;
    public Integer legacyStatus;
    public Integer poolId;
    public String poolName;
    public Integer poolCount;
    public Integer parentImageId;
    public Integer parentCount;
    public Integer linkedUserId;
    public Integer linkedUserCount;
}
