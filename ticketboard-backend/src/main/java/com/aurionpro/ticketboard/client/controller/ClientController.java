package com.aurionpro.ticketboard.client.controller;

import com.aurionpro.ticketboard.client.dto.ClientDto;
import com.aurionpro.ticketboard.client.service.ClientService;
import com.aurionpro.ticketboard.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientDto>>> getAllClients() {
        List<ClientDto> clients = clientService.getAllClients();
        return ResponseEntity.ok(ApiResponse.ok("Clients fetched successfully", clients));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientDto>> getClientById(@PathVariable Long id) {
        ClientDto client = clientService.getClientById(id);
        return ResponseEntity.ok(ApiResponse.ok("Client fetched successfully", client));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('admin:master-data', 'project:edit')")
    public ResponseEntity<ApiResponse<ClientDto>> createClient(@Valid @RequestBody ClientDto dto) {
        ClientDto created = clientService.createClient(dto);
        return ResponseEntity.ok(ApiResponse.ok("Client created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('admin:master-data', 'project:edit')")
    public ResponseEntity<ApiResponse<ClientDto>> updateClient(@PathVariable Long id, @Valid @RequestBody ClientDto dto) {
        ClientDto updated = clientService.updateClient(id, dto);
        return ResponseEntity.ok(ApiResponse.ok("Client updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:master-data')")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.ok(ApiResponse.ok("Client deleted successfully", null));
    }
}
