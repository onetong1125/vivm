package com.ttt.vivm.controller;

import com.ttt.vivm.cycle.port.ExternalOperationException;
import com.ttt.vivm.dto.VmDetailsDTO;
import com.ttt.vivm.model.Img;
import com.ttt.vivm.model.User;
import com.ttt.vivm.model.Vm;
import com.ttt.vivm.service.ImageService;
import com.ttt.vivm.service.VmService;
import com.ttt.vivm.dto.VmDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vms")
public class VMController {
    @Autowired
    private VmService vmService;
    @Autowired
    private ImageService imageService;

    // --------------------------------创建虚拟机-----------------------------------
    /**
     * 创建模版虚拟机
     * @param vmName 虚拟机名称
     * @param size 虚拟机磁盘大小
     * @param isoFileName 虚拟机要导入的iso镜像文件名称
     * @param currentUser 创建虚拟机的用户
     * @return
     */
    @PostMapping("/createModel")
    public ResponseEntity<?> createModelVm(
            @RequestParam String vmName,
            @RequestParam Long size,
            @RequestParam String isoFileName,
            @AuthenticationPrincipal User currentUser
    ) {
        System.out.println("-----------------API: vms/createModel-----------------");
        // 打印请求参数
        System.out.println("Received data: vmName=" + vmName + ", size=" + size + ", isoFileName=" + isoFileName);
        Map<String, Object> response = new HashMap<>();
        // 1. 查询数据库看vmName是否重复
        if (vmService.existsVm(vmName)) {
            response.put("status", "error");
            response.put("message", "VM name already exists. Please choose another name.");
            return ResponseEntity.status(400).body(response);       // 400 Bad Request
        }
        try {
            // 2. 创建虚拟镜像（包含创建虚拟镜像池，和模版镜像同名）
            Img img = imageService.createBaseImage(vmName, vmName, size, currentUser);
            // 3. 创建虚拟机
            vmService.createModelVm(vmName, img, isoFileName, currentUser);
            response.put("status", "success");
            response.put("message", "VM created successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 3. 捕获创建失败的异常
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 创建克隆虚拟机
     * @param vmName 虚拟机名称
     * @param imgId 使用的虚拟镜像名称
     * @param currentUser 创建虚拟机的用户
     * @return
     */
    @PostMapping("/createClone")
    public ResponseEntity<?> createCloneVm(
            @RequestParam String vmName,
            @RequestParam String imgId,
            @AuthenticationPrincipal User currentUser
    ) {
        System.out.println("-----------------API: vms/createClone-----------------");
        Map<String, Object> response = new HashMap<>();
        // 1. 查询数据库看vmName是否重复
        if (vmService.existsVm(vmName)) {
            response.put("status", "error");
            response.put("message", "VM name already exists. Please choose another name.");
            return ResponseEntity.status(400).body(response);       // 400 Bad Request
        }
        try {
            // 2. 根据imgId在数据库img
            int id = Integer.parseInt(imgId);
            Optional<Img> imgOptional = imageService.findById(id);
            if (imgOptional.isEmpty()) {
                throw new Exception("Image id doesn't exist");
            }
            // todo 3. 检查虚拟镜像是否可用（暂时只检查当前增量，不查找父镜像）
            if (!imageService.accessibleImageById(id)) {
                throw new Exception("Image exists but inaccessible!");
            }
            Img img = imgOptional.get();
            // 4. 创建虚拟机
            System.out.println("Clone vm uses image in: " + img.getVmImgPath());
            vmService.createCloneVm(vmName, img, currentUser);
            response.put("status", "success");
            response.put("message", "VM created successfully!");
            return ResponseEntity.ok(response);
        } catch (ExternalOperationException e) {
            response.put("status", "error");
            response.put("message", "External operation failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    // --------------------------------获取虚拟机详情-----------------------------------
    /**
     * 获取虚拟机详情
     * @param id 虚拟机ID
     * @return 虚拟机详细信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getVirtualMachineDetails(@PathVariable String id) {
        try {
            Optional<Vm> vmOptional = vmService.findById(Integer.parseInt(id));

            if (vmOptional.isPresent()) {
                Vm vm = vmOptional.get();
                VmDetailsDTO vmDetailsDTO = new VmDetailsDTO(vm); // 转换成 DTO
                return ResponseEntity.ok(vmDetailsDTO);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body("获取虚拟机详情失败：" + e.getMessage());
        }
    }

    // --------------------------------操作虚拟机-----------------------------------

    /**
     * 启动虚拟机
     * @param vmName 虚拟机名称
     * @return
     */
    @PostMapping("/{vmName}/start")
    public ResponseEntity<?> startVm(@PathVariable String vmName) {
        try {
            String result = vmService.startVm(vmName);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (ExternalOperationException e) {
            return handleExternalOperationError(e);
        }
    }

    /**
     * 关闭虚拟机
     * @param vmName 虚拟机名称
     * @return
     */
    @PostMapping("/{vmName}/shutdown")
    public ResponseEntity<?> shutdownVm(@PathVariable String vmName) {
        try {
            String result = vmService.shutdownVm(vmName);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (ExternalOperationException e) {
            return handleExternalOperationError(e);
        }
    }

    /**
     * 重启虚拟机
     * @param vmName 虚拟机名称
     * @return
     */
    @PostMapping("/{vmName}/restart")
    public ResponseEntity<?> restartVm(@PathVariable String vmName) {
        try {
            String result = vmService.restartVm(vmName);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (ExternalOperationException e) {
            return handleExternalOperationError(e);
        }
    }


    /**
     * 删除虚拟机
     * @param vmName 虚拟机名称
     * @return
     */
    @DeleteMapping("/{vmName}/delete")
    public ResponseEntity<?> deleteVm(@PathVariable String vmName) {
        try {
            String result = vmService.deleteVm(vmName);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", result);
            return ResponseEntity.ok(response);
        } catch (ExternalOperationException e) {
            return handleExternalOperationError(e);
        }
    }

    // --------------------------------获取虚拟机列表-----------------------------------

    @GetMapping("/list")
    public ResponseEntity<List<VmDTO>> listVms(
            @RequestParam(required = false, defaultValue = "") String search
    ) {
        List<Vm> vms;
        if (search.isEmpty()) {
            vms = vmService.getAllVms();
        }
        else {
            vms = vmService.searchVmsByName(search);
        }
        List<VmDTO> vmDTOS = vms.stream().map(VmDTO::new).collect(Collectors.toList());
        return ResponseEntity.ok(vmDTOS);
    }

//    /**
//     * 获取可用于创建克隆虚拟机的虚拟机列表（只要不在运行态即可）
//     * @return
//     */
//    @GetMapping("/models")
//    public ResponseEntity<List<VmDTO>> getModelVms() {
//        List<Vm> modelVms = vmService.getAllAvailableModelVms();
//        System.out.println("Model VMs: " + modelVms); // 输出返回的虚拟机数据
//        List<VmDTO> vmDTOS = modelVms.stream().map(VmDTO::new).collect(Collectors.toList());
//        return ResponseEntity.ok(vmDTOS);
//    }

    private ResponseEntity<?> handleExternalOperationError(ExternalOperationException e) {
        return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "outcome", e.outcome().name(),
                "message", e.getMessage()));
    }
}
