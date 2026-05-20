package com.commerce.bff.security;

import com.commerce.bff.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String adminId) throws UsernameNotFoundException {
        return adminRepository.findByAdminId(adminId)
                .map(AdminDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found: " + adminId));
    }
}
