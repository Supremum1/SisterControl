package com.sistercontrol.server.service;

import com.sistercontrol.server.dto.ApiDtos.ParentUsageResponse;
import com.sistercontrol.server.dto.ApiDtos.ScreenTimePolicyRequest;
import com.sistercontrol.server.dto.ApiDtos.ScreenTimePolicyResponse;
import com.sistercontrol.server.dto.ApiDtos.UsageUploadRequest;
import com.sistercontrol.server.exception.ApiException;
import com.sistercontrol.server.repository.ConnectionRepository;
import com.sistercontrol.server.repository.ScreenTimeRepository;
import com.sistercontrol.server.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ScreenTimeService {
    private final ScreenTimeRepository screenTime;
    private final ConnectionRepository connections;

    public ScreenTimeService(ScreenTimeRepository screenTime, ConnectionRepository connections) {
        this.screenTime = screenTime;
        this.connections = connections;
    }

    @Transactional
    public void saveParentPolicy(AuthenticatedUser user, ScreenTimePolicyRequest request) {
        requireParent(user);
        UUID childId = connections.findChildIdByParent(user.id())
                .orElseThrow(() -> ApiException.badRequest("No child connected"));
        screenTime.savePolicy(user.id(), childId, request.totalLimitMin(), request.rules());
    }

    public ScreenTimePolicyResponse parentPolicy(AuthenticatedUser user) {
        requireParent(user);
        return connections.findChildIdByParent(user.id())
                .map(this::policyForChild)
                .orElseGet(() -> new ScreenTimePolicyResponse(0, List.of()));
    }

    public ScreenTimePolicyResponse childPolicy(AuthenticatedUser user) {
        requireChild(user);
        return policyForChild(user.id());
    }

    @Transactional
    public void uploadUsage(AuthenticatedUser user, UsageUploadRequest request) {
        requireChild(user);
        if (request.usage() == null) {
            throw ApiException.badRequest("day and usage[] required");
        }
        request.usage().stream()
                .filter(row -> row.packageName() != null && !row.packageName().isBlank())
                .forEach(row -> screenTime.upsertUsage(
                        user.id(),
                        request.day(),
                        row.packageName().trim(),
                        row.usedSec()
                ));
    }

    public ParentUsageResponse parentUsage(AuthenticatedUser user, LocalDate day) {
        requireParent(user);
        LocalDate usageDay = day == null ? LocalDate.now() : day;
        UUID childId = connections.findChildIdByParent(user.id()).orElse(null);
        if (childId == null) {
            return new ParentUsageResponse(usageDay, List.of());
        }
        return screenTime.findPolicyByChild(childId)
                .map(policy -> new ParentUsageResponse(
                        usageDay,
                        screenTime.findUsageForParent(childId, policy.id(), usageDay)
                ))
                .orElseGet(() -> new ParentUsageResponse(usageDay, List.of()));
    }

    private ScreenTimePolicyResponse policyForChild(UUID childId) {
        return screenTime.findPolicyByChild(childId)
                .map(policy -> new ScreenTimePolicyResponse(
                        policy.totalLimitMin(),
                        screenTime.findRules(policy.id())
                ))
                .orElseGet(() -> new ScreenTimePolicyResponse(0, List.of()));
    }

    private void requireParent(AuthenticatedUser user) {
        if (!user.isParent()) {
            throw ApiException.forbidden();
        }
    }

    private void requireChild(AuthenticatedUser user) {
        if (!user.isChild()) {
            throw ApiException.forbidden();
        }
    }
}
