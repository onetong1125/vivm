package com.ttt.vivm.dao.identity.record;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

@TableName("`user`")
public class UserRecord {

    @TableId(value = "user_id", type = IdType.AUTO)
    private Integer id;
    @TableField("user_name")
    private String userName;
    @TableField("user_pwd")
    private String passwordHash;
    private int role;
    @TableField("oss_space")
    private long ossSpace;
    private String email;
    private BigDecimal balance;
    @TableField("account_status")
    private String accountStatus;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public long getOssSpace() {
        return ossSpace;
    }

    public void setOssSpace(long ossSpace) {
        this.ossSpace = ossSpace;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }
}
