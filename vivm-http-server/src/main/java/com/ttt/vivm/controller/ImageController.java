package com.ttt.vivm.controller;

import com.ttt.vivm.cycle.port.ExternalOperationException;
import com.ttt.vivm.dto.ImgDTO;
import com.ttt.vivm.dto.ImgDetailsDTO;
import com.ttt.vivm.model.Img;
import com.ttt.vivm.model.User;
import com.ttt.vivm.service.ImageService;
import com.ttt.vivm.service.PoolService;
import com.ttt.vivm.service.UserImageService;
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
@RequestMapping("/api/imgs")
public class ImageController {
    @Autowired
    private ImageService imageService;
    @Autowired
    private UserImageService userImageService;
    @Autowired
    private PoolService poolService;

    /**
     * 创建克隆镜像
     * @param imgName 克隆镜像名
     * @param parentName 父镜像名称，可能是克隆或模版
     * @param currentUser 操作用户
     * @return
     */
    @PostMapping("/createClone")
    public ResponseEntity<?> createClone(
            @RequestParam String imgName,
            @RequestParam String parentName,
            @AuthenticationPrincipal User currentUser
    ) {
        System.out.println("-----------------API: imgs/createClone-----------------");
        // 打印请求参数
        System.out.println("Received data: imgName=" + imgName + ", parentName=" + parentName);
        Map<String, Object> response = new HashMap<>();
        // 1. 查询数据库看imgName是否重复(因为如果重复，运行qemu命令仍能正常创建，覆盖原镜像)
        if (imageService.existsImage(imgName)) {
            response.put("status", "error");
            response.put("message", "Image name already exists. Please choose another name.");
            return ResponseEntity.status(400).body(response);       // 400 Bad Request
        }
        // 2. 查询数据库看parentImg是否已被删除
        if (imageService.isDeletedByImgName(parentName)) {
            response.put("status", "error");
            response.put("message", "Parent image is deleted.");
            return ResponseEntity.status(400).body(response);
        }
        try {
            // 3. 根据parentName找到imagePath。不存在报错
            String parentPath = imageService.getImagePathByImgName(parentName);

            // 4. 找到parent的poolName
            Optional<Img> imgOptional = imageService.findByImgName(parentName);
            String poolName = poolService.findPoolNameByImageId(imgOptional.get().getImgId());  // 不用检查，因为第3步确定一定存在

            // 5. 使用imageService创建image
            imageService.createDeltaImage(poolName, imgName, parentPath, currentUser);
            response.put("status", "success");
            response.put("message", "Clone image created successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 删除虚拟镜像
     * @param imgId 镜像id
     * @param currentUser 执行操作的用户
     * @return
     */
    @PostMapping("/deleteImage")
    public ResponseEntity<?> deleteImage(
            @RequestParam String imgId,
            @AuthenticationPrincipal User currentUser
    ) {
        System.out.println("-----------------API: imgs/deleteImage-----------------");
        // 打印请求参数
        System.out.println("Received data: imgId=" + imgId);
        Map<String, Object> response = new HashMap<>();
        int id = Integer.parseInt(imgId);
        // 检查权限
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !userImageService.existUserImage(currentUser.getUserId(), id)) {
            // 检查普通用户是否拥有此镜像的权限
            response.put("status", "error");
            response.put("message", "Authentication failure");
            return ResponseEntity.status(403).body(response);
        }
        // 有权限，执行删除
        try {
            imageService.deleteImageByImgId(id);
            response.put("status", "success");
            response.put("message", "Image deleted successfully!");
            return ResponseEntity.ok(response);
        }
        catch (ExternalOperationException e) {
            response.put("status", "error");
            response.put("outcome", e.outcome().name());
            response.put("message", "External operation failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
        catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }

    /**
     * (按要求获取虚拟镜像列表)
     * @param search 搜索关键词
     * @return 虚拟镜像数据传递对象列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<ImgDTO>> listImages(
            @RequestParam(required = false, defaultValue = "") String search
    ) {
        List<Img> images;
        if (search.isEmpty()) {
            images = imageService.getAllImages();
        }
        else {
            images = imageService.searchImagesByName(search);
        }
        List<ImgDTO> imgDTOS = images.stream().map(ImgDTO::new).collect(Collectors.toList());
        return ResponseEntity.ok(imgDTOS);
    }

    // --------------------------------获取虚拟镜像详情-----------------------------------
    /**
     * 获取虚拟镜像详情
     * @param id 虚拟镜像ID
     * @return 虚拟镜像详细信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getImageDetails(@PathVariable String id) {
        try {
            Optional<Img> imgOptional = imageService.findById(Integer.parseInt(id));

            if (imgOptional.isPresent()) {
                Img img = imgOptional.get();
                ImgDetailsDTO imgDetailsDTO = new ImgDetailsDTO(img);
                return ResponseEntity.ok(imgDetailsDTO);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body("获取虚拟镜像详情失败：" + e.getMessage());
        }
    }
}
