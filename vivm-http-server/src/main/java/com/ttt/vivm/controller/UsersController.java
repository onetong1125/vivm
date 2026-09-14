package com.ttt.vivm.controller;

import com.ttt.vivm.cycle.domain.identity.model.entity.UserE;
import com.ttt.vivm.dto.UserOptDTO;
import com.ttt.vivm.dto.UserViewDTO;
import com.ttt.vivm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UsersController {
    @Autowired
    private UserService userService;

    @GetMapping
    public Page<UserViewDTO> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        return userService.getUsers(page, size, keyword).map(UserViewDTO::new);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(
            @PathVariable String userId
    ) {
        System.out.println("-----------------API: users/getUser-----------------");
        int id = Integer.parseInt(userId);
        System.out.println("parameter id: " + id);
        Optional<UserE> optionalUser = userService.findById(id);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("用户不存在");
        }

        UserE user = optionalUser.get();
        return ResponseEntity.ok(new UserViewDTO(user));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(
            @PathVariable int userId
    ) {
        Map<String, Object> response = new HashMap<>();
        userService.deleteUserById(userId);
        response.put("status", "success");
        response.put("message", "User deleted successfully!");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Integer id, @RequestBody UserOptDTO userOptDto) {
        userService.updateUser(id, userOptDto);
        return ResponseEntity.ok(Map.of("status", "success", "message", "更新成功"));
    }
}
