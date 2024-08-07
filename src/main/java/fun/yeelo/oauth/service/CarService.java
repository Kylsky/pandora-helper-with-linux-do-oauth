package fun.yeelo.oauth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import fun.yeelo.oauth.dao.AccountMapper;
import fun.yeelo.oauth.dao.CarMapper;
import fun.yeelo.oauth.dao.ShareMapper;
import fun.yeelo.oauth.domain.Account;
import fun.yeelo.oauth.domain.CarApply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarService extends ServiceImpl<CarMapper, CarApply> implements IService<CarApply> {

}

