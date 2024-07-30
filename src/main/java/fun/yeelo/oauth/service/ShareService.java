package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.yeelo.oauth.dao.ShareMapper;
import fun.yeelo.oauth.domain.Share;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class ShareService extends ServiceImpl<ShareMapper, Share> implements IService<Share> {
    @Autowired
    private ShareMapper shareMapper;


    public List<Share> findAll() {
        return shareMapper.selectList(null);
    }

    public Share findById(Integer id) {
        return shareMapper.selectById(id);
    }

    public Share getByUserName(String username) {
        List<Share> shares = shareMapper.selectList(new LambdaQueryWrapper<Share>().eq(Share::getUniqueName, username));
        if (CollectionUtils.isEmpty(shares)) {
            return null;
        }
        return shares.get(0);
    }


}
