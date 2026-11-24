package com.ttt.vivm.controller;

import com.ttt.vivm.cycle.domain.operation.repo.OperationRepository;
import com.ttt.vivm.dto.OperationViewDTO;
import com.ttt.vivm.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations")
public class OperationController {
    private final OperationRepository operations;

    public OperationController(OperationRepository operations) {
        this.operations = operations;
    }

    @GetMapping("/{id}")
    public ResponseEntity<OperationViewDTO> findById(@PathVariable String id, @AuthenticationPrincipal User actor) {
        return ResponseEntity.of(operations.findByIdAndActorId(id, actor.getUserId()).map(OperationViewDTO::new));
    }
}
