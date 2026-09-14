package com.aurionpro.ticketboard.client.service;

import com.aurionpro.ticketboard.client.dto.ClientDto;
import com.aurionpro.ticketboard.client.entity.Client;
import com.aurionpro.ticketboard.client.repository.ClientRepository;
import com.aurionpro.ticketboard.common.exception.BadRequestException;
import com.aurionpro.ticketboard.common.exception.ResourceNotFoundException;
import com.aurionpro.ticketboard.user.entity.User;
import com.aurionpro.ticketboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ClientDto> getAllClients() {
        return clientRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClientDto getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", id));
        return mapToDto(client);
    }

    @Transactional
    public ClientDto createClient(ClientDto dto) {
        if (clientRepository.existsByClientCode(dto.getClientCode())) {
            throw new BadRequestException("Client code already exists: " + dto.getClientCode());
        }

        User accountManager = null;
        if (dto.getAccountManagerId() != null) {
            accountManager = userRepository.findById(dto.getAccountManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getAccountManagerId()));
        }

        Client client = Client.builder()
                .clientCode(dto.getClientCode().toUpperCase().trim())
                .name(dto.getName())
                .contactPerson(dto.getContactPerson())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .accountManager(accountManager)
                .build();

        return mapToDto(clientRepository.save(client));
    }

    @Transactional
    public ClientDto updateClient(Long id, ClientDto dto) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client", "id", id));

        client.setName(dto.getName());
        client.setContactPerson(dto.getContactPerson());
        client.setEmail(dto.getEmail());
        client.setPhone(dto.getPhone());
        client.setAddress(dto.getAddress());
        if (dto.getStatus() != null) {
            client.setStatus(dto.getStatus());
        }

        if (dto.getAccountManagerId() != null) {
            User accountManager = userRepository.findById(dto.getAccountManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", dto.getAccountManagerId()));
            client.setAccountManager(accountManager);
        }

        return mapToDto(clientRepository.save(client));
    }

    @Transactional
    public void deleteClient(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Client", "id", id);
        }
        clientRepository.deleteById(id);
    }

    public ClientDto mapToDto(Client client) {
        return ClientDto.builder()
                .id(client.getId())
                .clientCode(client.getClientCode())
                .name(client.getName())
                .contactPerson(client.getContactPerson())
                .email(client.getEmail())
                .phone(client.getPhone())
                .address(client.getAddress())
                .status(client.getStatus())
                .accountManagerId(client.getAccountManager() != null ? client.getAccountManager().getId() : null)
                .accountManagerName(client.getAccountManager() != null ? client.getAccountManager().getFullName() : null)
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
    }
}
