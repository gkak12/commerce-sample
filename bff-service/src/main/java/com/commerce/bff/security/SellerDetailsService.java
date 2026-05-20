package com.commerce.bff.security;

import com.commerce.bff.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SellerDetailsService implements UserDetailsService {

    private final SellerRepository sellerRepository;

    @Override
    public UserDetails loadUserByUsername(String sellerId) throws UsernameNotFoundException {
        return sellerRepository.findBySellerId(sellerId)
                .map(SellerDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Seller not found: " + sellerId));
    }
}
