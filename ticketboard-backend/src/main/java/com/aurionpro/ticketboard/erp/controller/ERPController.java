package com.aurionpro.ticketboard.erp.controller;

import com.aurionpro.ticketboard.common.response.ApiResponse;
import com.aurionpro.ticketboard.erp.entity.ERPAsset;
import com.aurionpro.ticketboard.erp.entity.ExpenseClaim;
import com.aurionpro.ticketboard.erp.entity.LeaveRequest;
import com.aurionpro.ticketboard.erp.entity.OKRGoal;
import com.aurionpro.ticketboard.erp.repository.ERPAssetRepository;
import com.aurionpro.ticketboard.erp.repository.ExpenseClaimRepository;
import com.aurionpro.ticketboard.erp.repository.LeaveRequestRepository;
import com.aurionpro.ticketboard.erp.repository.OKRGoalRepository;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import com.aurionpro.ticketboard.websocket.WorkspaceRealtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/erp")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ERPController {

    private final ERPAssetRepository assetRepository;
    private final ExpenseClaimRepository expenseClaimRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final OKRGoalRepository okrGoalRepository;
    private final UserRepository userRepository;
    private final WorkspaceRealtimeService realtimeService;

    @GetMapping("/assets")
    public ResponseEntity<ApiResponse<List<ERPAsset>>> getAssets() {
        Long userId = currentUserId();
        List<ERPAsset> assets = userId != null
                ? assetRepository.findByUserId(userId)
                : assetRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("ERP assets retrieved", assets));
    }

    @PostMapping("/assets")
    public ResponseEntity<ApiResponse<ERPAsset>> createAssetRequest(@RequestBody ERPAsset asset) {
        if (asset.getStatus() == null) asset.setStatus("PENDING_RETURN");
        if (asset.getAssetCode() == null) asset.setAssetCode("AST-" + System.currentTimeMillis() % 10000);
        asset.setAssignedDate(asset.getAssignedDate() != null ? asset.getAssignedDate() : LocalDate.now());
        asset.setUserId(currentUserId());
        ERPAsset saved = assetRepository.save(asset);
        realtimeService.notifyDashboardSync(currentUserId());
        return ResponseEntity.ok(ApiResponse.success("Asset request submitted", saved));
    }

    @GetMapping("/expenses")
    public ResponseEntity<ApiResponse<List<ExpenseClaim>>> getExpenseClaims() {
        Long userId = currentUserId();
        List<ExpenseClaim> claims = userId != null
                ? expenseClaimRepository.findByUserId(userId)
                : expenseClaimRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Expense claims retrieved", claims));
    }

    @PostMapping("/expenses")
    public ResponseEntity<ApiResponse<ExpenseClaim>> submitExpense(@RequestBody ExpenseClaim claim) {
        if (claim.getClaimCode() == null) claim.setClaimCode("EXP-" + System.currentTimeMillis() % 1000);
        if (claim.getStatus() == null) claim.setStatus("PENDING");
        claim.setClaimDate(LocalDate.now());
        claim.setUserId(currentUserId());
        ExpenseClaim saved = expenseClaimRepository.save(claim);
        realtimeService.notifyDashboardSync(currentUserId());
        return ResponseEntity.ok(ApiResponse.success("Expense claim submitted", saved));
    }

    @GetMapping("/leaves")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> getLeaveRequests() {
        Long userId = currentUserId();
        List<LeaveRequest> requests = userId != null
                ? leaveRequestRepository.findByUserId(userId)
                : leaveRequestRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Leave requests retrieved", requests));
    }

    @PostMapping("/leaves")
    public ResponseEntity<ApiResponse<LeaveRequest>> submitLeave(@RequestBody LeaveRequest req) {
        if (req.getRequestCode() == null) req.setRequestCode("LR-" + System.currentTimeMillis() % 1000);
        if (req.getStatus() == null) req.setStatus("PENDING");
        req.setUserId(currentUserId());
        LeaveRequest saved = leaveRequestRepository.save(req);
        realtimeService.notifyDashboardSync(currentUserId());
        return ResponseEntity.ok(ApiResponse.success("Leave request submitted", saved));
    }

    @GetMapping("/okrs")
    public ResponseEntity<ApiResponse<List<OKRGoal>>> getOKRGoals() {
        Long userId = currentUserId();
        List<OKRGoal> okrs = userId != null
                ? okrGoalRepository.findByUserId(userId)
                : okrGoalRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("OKR goals retrieved", okrs));
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
    }
}