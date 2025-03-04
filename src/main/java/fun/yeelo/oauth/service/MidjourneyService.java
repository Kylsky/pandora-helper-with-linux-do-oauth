package fun.yeelo.oauth.service;

import com.alibaba.fastjson.JSONObject;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.midjourney.*;
import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.domain.share.ShareVO;
import fun.yeelo.oauth.utils.ConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.apache.bcel.generic.RET;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MidjourneyService {
    @Autowired
    private ShareService shareService;
    @Value("${midjourney.url}")
    private String mjUrl;

    @Value("${midjourney.key}")
    private String mjKey;

    @Value("${midjourney.enable}")
    private Boolean mjEnable;

    public HttpResult<UserResponse> getUsers(String username) {
        Share byUserName = shareService.getByUserName(username);
        Boolean useCustomMjConfig = StringUtils.hasText(byUserName.getMjProxyUrl());
        if (!useCustomMjConfig && (mjEnable == null || !mjEnable)) {
            return HttpResult.error("未启用MJ");
        }
        Share admin = shareService.getById(1);
        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", "application/json, text/plain, */*");
        headers.set("accept-language", "zh-CN");
        headers.set("content-type", "application/json");
        if (useCustomMjConfig) {
            headers.set("mj-api-secret", byUserName.getMjProxyKey());
        } else {
            headers.set("mj-api-secret", StringUtils.hasText(mjKey) ? mjKey : admin.getId() + "+" + admin.getUniqueName() + "+" + admin.getPassword().substring(0, 10));
        }
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0");

        // 创建请求体
        UserRequest requestBody = new UserRequest();

        // 设置分页信息
        Pagination pagination = new Pagination();
        pagination.setCurrent(1);
        pagination.setPageSize(999);
        requestBody.setPagination(pagination);

        // 设置排序信息
        Sort sort = new Sort();
        sort.setPredicate("");
        sort.setReverse(true);
        requestBody.setSort(sort);

        // 设置搜索信息
        Search search = new Search();
        if (StringUtils.hasText(username)) {
            search.setName(username);
        }
        search.setCurrent(1);
        search.setPageSize(999);
        search.setPageNumber(0);
        requestBody.setSearch(search);

        // 创建HTTP实体，包含头部和请求体
        HttpEntity<UserRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        // 发送请求并返回响应
        ResponseEntity<String> exchange = new RestTemplate().postForEntity(
                (!useCustomMjConfig ? mjUrl : byUserName.getMjProxyUrl()) + "/mj/admin/users",
                requestEntity,
                String.class
        );
        try {
            List<Share> allUser = shareService.list();
            List<ShareVO> allUsers = ConvertUtil.convertList(allUser, ShareVO.class);
            UserResponse userResponse = JSONObject.parseObject(exchange.getBody(), UserResponse.class);
            Map<String, User> mjUserMap = userResponse.getList().stream().collect(Collectors.toMap(User::getName, Function.identity()));

            if (!useCustomMjConfig) {
                allUsers.stream().filter(e -> e.getMjUserId() == null && mjUserMap.containsKey(e.getUniqueName())).forEach(e -> {
                    ShareVO share = new ShareVO();
                    share.setId(e.getId());
                    share.setMjUserId(mjUserMap.get(e.getUniqueName()).getId());
                    shareService.updateById(share);
                });
            }

            return HttpResult.success(userResponse);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return HttpResult.error("获取用户列表失败");
        }
    }

    public HttpResult<UserResponse> addUser(Share share, String status) {
        if (mjEnable == null || !mjEnable) {
            return HttpResult.error("未启用MJ");
        }
        CompletableFuture.runAsync(() -> {
            Share admin = shareService.getById(1);
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json, text/plain, */*");
            headers.set("accept-language", "zh-CN");
            headers.set("content-type", "application/json");
            headers.set("mj-api-secret", StringUtils.hasText(mjKey) ? mjKey : admin.getId() + "+" + admin.getUniqueName() + "+" + admin.getPassword().substring(0, 10));
            headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0");

            // 创建请求体
            // 创建HTTP实体，包含头部和请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("name", share.getUniqueName());
            requestBody.put("role", "USER");
            requestBody.put("status", status);
            requestBody.put("token", share.getId() + "+" + share.getUniqueName() + "+" + share.getPassword().substring(0, 10));
            HttpEntity<JSONObject> requestEntity = new HttpEntity<>(requestBody, headers);

            // 发送请求并返回响应
            try {
                ResponseEntity<String> response = new RestTemplate().postForEntity(
                        mjUrl + "/mj/admin/user",
                        requestEntity,
                        String.class
                );
                log.info(response.getBody());
                JSONObject userResponse = JSONObject.parseObject(response.getBody(), JSONObject.class);
                if (userResponse.containsKey("success") && userResponse.getBoolean("success")) {
                    getUsers(share.getUniqueName());
                }
            } catch (Exception e) {
                log.error("添加MJ用户失败", e.getCause());
            }
        });

        return HttpResult.success();
    }

    public HttpResult<UserResponse> deleteUser(String mjUserId) {
        if (mjEnable == null || !mjEnable || mjUserId == null) {
            return HttpResult.error("未启用MJ");
        }
        CompletableFuture.runAsync(() -> {
            Share admin = shareService.getById(1);
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json, text/plain, */*");
            headers.set("accept-language", "zh-CN");
            headers.set("content-type", "application/json");
            headers.set("mj-api-secret", StringUtils.hasText(mjKey) ? mjKey : admin.getId() + "+" + admin.getUniqueName() + "+" + admin.getPassword().substring(0, 10));
            headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0");

            // 创建请求体
            // 创建HTTP实体，包含头部和请求体
            HttpEntity<JSONObject> requestEntity = new HttpEntity<>(null, headers);

            // 发送请求并返回响应
            try {
                log.info("删除MJ用户, mjUserId:{}", mjUserId);
                new RestTemplate().exchange(
                        mjUrl + "/mj/admin/user/" + mjUserId,
                        HttpMethod.DELETE,
                        requestEntity,
                        String.class
                );
            } catch (Exception e) {
                log.error("删除MJ用户失败", e.getCause());
            }
        });
        return HttpResult.success();
    }

    public HttpResult<UserResponse> updateUser(Share share, String status) {
        if (mjEnable == null || !mjEnable || share.getMjUserId() == null) {
            return HttpResult.error("未启用MJ");
        }
        CompletableFuture.runAsync(() -> {
            Share admin = shareService.getById(1);
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json, text/plain, */*");
            headers.set("accept-language", "zh-CN");
            headers.set("content-type", "application/json");
            headers.set("mj-api-secret", StringUtils.hasText(mjKey) ? mjKey : admin.getId() + "+" + admin.getUniqueName() + "+" + admin.getPassword().substring(0, 10));
            headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0");

            // 创建请求体
            // 创建HTTP实体，包含头部和请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("id", share.getMjUserId());
            if (status != null) {
                requestBody.put("status", status);
            }
            requestBody.put("token", share.getId() + "+" + share.getUniqueName() + "+" + share.getPassword().substring(0, 10));
            requestBody.put("name", share.getUniqueName());
            HttpEntity<JSONObject> requestEntity = new HttpEntity<>(requestBody, headers);
            // 发送请求并返回响应
            try {

                ResponseEntity<String> exchange = new RestTemplate().postForEntity(
                        mjUrl + "/mj/admin/user",
                        requestEntity,
                        String.class
                );
                JSONObject userResponse = JSONObject.parseObject(exchange.getBody(), JSONObject.class);
                if (userResponse.containsKey("success") && userResponse.getBoolean("success")) {
                    getUsers(share.getUniqueName());
                }
            } catch (Exception e) {
                log.error("更新MJ用户失败", e.getCause());
            }
        });
        return HttpResult.success();
    }
}
