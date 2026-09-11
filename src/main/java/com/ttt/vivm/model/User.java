package com.ttt.vivm.model;

import jakarta.persistence.*;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

@Entity
@Table(name = "user")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userId")
    private int userId;

    @Column(unique = true, nullable = false, name = "user_name")
    private String userName;

    @Column(nullable = false, name = "user_pwd")
    private String userPassword;

    @Column(nullable = false)
    private int role;

    @Column(nullable = false, columnDefinition = "BIGINT", name = "oss_space")
    private long ossSpace;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false, columnDefinition = "DECIMAL(9,2)")
    private BigDecimal balance;


    @Transient  // 标记为非持久化字段
    private String confirmPassword;

    @OneToMany(mappedBy = "user")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<UserVm> userVms;

    @OneToMany(mappedBy = "user")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<UserImg> userImgs;

    // Getter and Setter

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
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


    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public Set<UserVm> getUserVms() {
        return userVms;
    }

    public void setUserVms(Set<UserVm> userVms) {
        this.userVms = userVms;
    }

    public Set<UserImg> getUserImgs() {
        return userImgs;
    }

    public void setUserImgs(Set<UserImg> userImgs) {
        this.userImgs = userImgs;
    }


    // 实现 UserDetails 接口方法 --------------------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 将 role 转换为 Spring Security 的权限标识（ROLE_ 前缀）
        String roleName = role == 1 ? "ROLE_ADMIN" : "ROLE_USER";
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public String getPassword() {
        return userPassword;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }


}
