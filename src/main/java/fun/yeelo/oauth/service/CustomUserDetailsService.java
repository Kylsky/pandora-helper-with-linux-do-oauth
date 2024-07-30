package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import fun.yeelo.oauth.dao.ShareMapper;
import fun.yeelo.oauth.domain.Share;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private ShareMapper shareMapper;

    @Override

    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Share share = shareMapper.selectOne(new LambdaQueryWrapper<Share>().eq(Share::getUniqueName, username));
        if (share == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        List<SimpleGrantedAuthority> authorities = Arrays.stream(new String[0])
                                                           .map(SimpleGrantedAuthority::new)
                                                           .collect(Collectors.toList());

        return org.springframework.security.core.userdetails.User.withUsername(username)
                                                                                 .password(share.getPassword())
                                                                                 .roles("USER")
                                                                                 .build();
    }
}
