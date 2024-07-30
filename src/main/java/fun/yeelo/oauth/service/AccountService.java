package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.yeelo.oauth.dao.AccountMapper;
import fun.yeelo.oauth.dao.ShareMapper;
import fun.yeelo.oauth.domain.Account;
import fun.yeelo.oauth.domain.Share;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class AccountService extends ServiceImpl<AccountMapper, Account> implements IService<Account> {

    @Autowired
    private AccountMapper accountMapper;
    @Autowired
    private ShareMapper shareMapper;

    public List<Account> findAll() {
        return accountMapper.selectList(null);
    }


    public Account findById(Integer id) {
        return accountMapper.selectById(id);
    }

    public List<Account> findByUserId(Integer userId) {
        return accountMapper.getByUserId(userId);
    }

    public void delete(Integer id) {
        accountMapper.deleteById(id);
    }

    public Account getById(Integer accountId) {
        return accountMapper.selectById(accountId);
    }
}

// Similar implementation for ShareService

