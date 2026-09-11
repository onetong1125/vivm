package com.ttt.vivm.controller;

import com.ttt.vivm.dto.PoolDTO;
import com.ttt.vivm.dto.VersionDTO;
import com.ttt.vivm.model.*;
import com.ttt.vivm.service.ImageService;
import com.ttt.vivm.service.PoolService;
import com.ttt.vivm.service.VersionService;
import com.ttt.vivm.service.VmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/version")
public class VersionController {
    @Autowired
    private VersionService versionService;
    @Autowired
    private ImageService imageService;
    @Autowired
    private PoolService poolService;
    @Autowired
    private VmService vmService;

    // --------------------------------管理版本-----------------------------------
    /**
     * 创建新版本
     * @param poolName 版本链名称（下拉框选择）
     * @param versionName 要创建的版本名
     * @param versionDescription 创建的版本描述
     * @param currentUser 进行操作的用户
     * @return
     */
    @PostMapping("/createBackup")
    public ResponseEntity<?> createBackup(
            @RequestParam String poolName,
            @RequestParam String versionName,
            @RequestParam String versionDescription,
            @AuthenticationPrincipal User currentUser
    ) {
        System.out.println("-----------------API: version/createBackup-----------------");
        // 打印请求参数
        System.out.println("Received data: poolName=" + poolName + ", versionName=" + versionName + ", versionDescription=" + versionDescription);
        Map<String, Object> response = new HashMap<>();
        // 1. 查询数据库看versionName是否重复
        if (versionService.existVersionByName(versionName)) {
            response.put("status", "error");
            response.put("message", "Version name already exists. Please choose another name.");
            return ResponseEntity.status(400).body(response);
        }
        try {
            // 2. 创建新备份
            versionService.createNewBackup(poolName, versionName, versionDescription);
            response.put("status", "success");
            response.put("message", "Version "+ versionName + " created successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 3. 捕获创建失败的异常
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 还原版本
     * @param poolName 版本链名称（下拉框选择）
     * @param versionId 要还原的版本id
     * @param currentUser 执行操作的用户
     * @return
     */
    @PostMapping("/restoreBackup")
    public ResponseEntity<?> restoreBackup(
            @RequestParam String poolName,
            @RequestParam Integer versionId,
            @AuthenticationPrincipal User currentUser
    ) {
        System.out.println("-----------------API: version/restoreBackup-----------------");
        // 打印请求参数
        System.out.println("Received data: poolName=" + poolName + ", versionId=" + versionId);
        Map<String, Object> response = new HashMap<>();
        Optional<Pool> poolOptional = poolService.findByName(poolName);
        Pool pool = poolOptional.get();         // 不需要检查因为pool是下拉框选的一定存在
        // 1. 检查在pool里要还原的版本id是否存在
        if (!versionService.existsByVersionIdAndPool(versionId, pool)) {
            response.put("status", "error");
            response.put("message", "Version id doesn't exists in pool!");
            return ResponseEntity.status(400).body(response);
        }
        // 2. 检查相关虚拟机是否关闭
        List<Img> imgs = imageService.findImgsByPool(pool);
        for (Img img : imgs) {
            if (vmService.hasRunningVm(img)) {
                // 如果有img的vm没关闭
                response.put("status", "error");
                response.put("message", "Image " + img.getImgName() + " has running VM. Please shutdown VM first.");
                return ResponseEntity.status(400).body(response);
            }
        }
        try {
            // 3. 还原版本
            Optional<Version> versionOptional = versionService.findById(versionId);
            Version version = versionOptional.get();
            versionService.restoreBackup(pool, version);
            response.put("status", "success");
            response.put("message", "Version "+ version.getName() + " restored successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 捕获还原中的异常
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // --------------------------------获取版本-----------------------------------

    /**
     * 获取/搜索版本列表
     * @param search 搜索关键词
     * @return 版本列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<VersionDTO>> listVersions(
            @RequestParam(required = false, defaultValue = "") String search
    ) {
        List<Version> versions;
        if (search.isEmpty()) {
            versions = versionService.getAllVersions();
        }
        else {
            List<Version> list1 = versionService.searchVersionsByName(search);
            List<Version> list2 = versionService.searchVersionsByDesc(search);
            List<Version> list3 = versionService.searchVersionsByPoolName(search);
            Set<Version> versionSet = new LinkedHashSet<>();
            versionSet.addAll(list1);
            versionSet.addAll(list2);
            versionSet.addAll(list3);
            versions = new ArrayList<>(versionSet);
        }
        List<VersionDTO> versionDTOS = versions.stream().map(VersionDTO::new).collect(Collectors.toList());
        return ResponseEntity.ok(versionDTOS);
    }

    // --------------------------------获取存储池-----------------------------------

    /**
     * 获取所有存储池
     * @return 存储池列表
     */
    @GetMapping("/pools")
    public ResponseEntity<List<PoolDTO>> listPools() {
        List<Pool> pools = poolService.getAllPools();
        List<PoolDTO> poolDTOS = pools.stream().map(PoolDTO::new).collect(Collectors.toList());
        return ResponseEntity.ok(poolDTOS);
    }
}
