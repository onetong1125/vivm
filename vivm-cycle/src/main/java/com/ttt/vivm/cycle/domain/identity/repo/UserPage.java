package com.ttt.vivm.cycle.domain.identity.repo;

import com.ttt.vivm.cycle.domain.identity.model.entity.UserE;

import java.util.List;

public record UserPage(List<UserE> users, long total) {
}
