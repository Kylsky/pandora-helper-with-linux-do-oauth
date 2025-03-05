package fun.yeelo.oauth.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.config.MirrorConfig;
import fun.yeelo.oauth.domain.account.Account;
import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.domain.share.ShareApiConfig;
import fun.yeelo.oauth.domain.share.ShareGptConfig;
import fun.yeelo.oauth.domain.share.ShareVO;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.ApiConfigService;
import fun.yeelo.oauth.service.MidjourneyService;
import fun.yeelo.oauth.service.ShareService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Controller
public class IndexController {
    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

}
