package fun.yeelo.oauth.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.dao.MidjourneyTaskMapper;
import fun.yeelo.oauth.domain.midjourney.*;
import fun.yeelo.oauth.domain.share.Share;
import fun.yeelo.oauth.service.MidjourneyService;
import fun.yeelo.oauth.service.MidjourneyTaskService;
import fun.yeelo.oauth.service.ShareService;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/mj")
public class MidJourneyController {
    @Value("${midjourney.url}")
    private String mjUrl;

    @Value("${midjourney.key}")
    private String mjKey;

    @Autowired
    private ShareService shareService;


    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private MidjourneyService midjourneyService;

    @Value("${midjourney.enable}")
    private Boolean mjEnable;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private MidjourneyTaskService midjourneyTaskService;
    @Qualifier("midjourneyTaskMapper")
    @Autowired
    private MidjourneyTaskMapper midjourneyTaskMapper;

    @GetMapping("/users")
    public HttpResult<UserResponse> getUsers(HttpServletRequest request, @RequestParam(required = false) String username) {
        if (!mjEnable) {
            return HttpResult.error("未启用MJ");
        }
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String myName = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(myName);
        if (user == null) {
            return HttpResult.error("无权访问数据");
        }
        HttpResult<UserResponse> users;
        if (StringUtils.hasText(user.getMjProxyUrl())) {
            UserResponse userResponse = new UserResponse();
            userResponse.setCustomMjConfig(true);
            users = HttpResult.success(userResponse);
        } else {
            users = midjourneyService.getUsers(user.getUniqueName());
            UserResponse data = users.getData();
            data.setList(data.getList().stream().filter(e -> user.getId().equals(1) || e.getName().equals(user.getUniqueName())).collect(Collectors.toList()));
        }
        return users;
    }

    @GetMapping("/tasks")
    public HttpResult<TaskResponse> getTasks(HttpServletRequest request,
                                          @RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer size) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String myName = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(myName);
        if (user == null) {
            return HttpResult.error("用户不存在");
        }
        Share admin = shareService.getById(1);
        HttpHeaders headers = new HttpHeaders();
        headers.set("accept", "application/json, text/plain, */*");
        headers.set("accept-language", "zh-CN");
        headers.set("content-type", "application/json");
        if (StringUtils.hasText(user.getMjProxyUrl())) {
            headers.set("mj-api-secret", user.getMjProxyKey());
        } else {
            headers.set("mj-api-secret", StringUtils.hasText(mjKey) ? mjKey : admin.getId() + "+" + admin.getUniqueName() + "+" + admin.getPassword().substring(0, 10));
        }
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0");

        // from to 查询,limit 10
        Page<MidjourneyTask> searchPage = new Page<>(page, size);
        LambdaQueryWrapper<MidjourneyTask> wrapper = new LambdaQueryWrapper<>();
        // 添加查询条件，例如按姓名模糊查询
        wrapper.eq(MidjourneyTask::getUsername, user.getUniqueName()).orderByDesc(MidjourneyTask::getCreateTime);

        Page<MidjourneyTask> tasks = midjourneyTaskService.page(searchPage, wrapper);

        UserRequest requestBody = new UserRequest();
        requestBody.setIds(tasks.getRecords().stream().map(MidjourneyTask::getTaskId).collect(Collectors.toList()));
        TaskResponse userResponse = new TaskResponse();
        // 设置分页信息
        //Pagination pagination = new Pagination();
        //pagination.setCurrent(page);
        //pagination.setPageSize(size);
        //requestBody.setPagination(pagination);

        // 查询条件
        //TaskInfo taskInfo = new TaskInfo();
        //taskInfo.setUserId(user.getMjUserId());
        //if (StringUtils.hasText(user.getMjProxyUrl())) {
        //    requestBody.setTaskInfo(taskInfo);
        //}

        // 设置排序信息
        //Sort sort = new Sort();
        //sort.setPredicate("");
        //sort.setReverse(true);
        //requestBody.setSort(sort);

        // 设置搜索信息
        //Search search = new Search();
        //search.setCurrent(page);
        //search.setPageSize(size);
        //search.setPageNumber(0);
        //requestBody.setSearch(search);

        // 创建HTTP实体，包含头部和请求体
        HttpEntity<UserRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        // 发送请求并返回响应
        //ResponseEntity<String> exchange = new RestTemplate().postForEntity(
        //        (StringUtils.hasText(user.getMjProxyUrl()) ? user.getMjProxyUrl() : mjUrl) + "/mj/admin/tasks",
        //        requestEntity,
        //        String.class
        //);

        ResponseEntity<String> exchange = new RestTemplate().postForEntity(
                (StringUtils.hasText(user.getMjProxyUrl()) ? user.getMjProxyUrl() : mjUrl) + "/mj/task/list-by-condition",
                requestEntity,
                String.class
        );
        try {
            JSONArray taskResponse = JSONArray.parseArray(exchange.getBody());
            String mjUserId = user.getMjUserId();
            if (taskResponse != null) {
                taskResponse.forEach(node -> {
                    JSONObject nodeJson = (JSONObject) node;
                    if (!user.getId().equals(1) && (!StringUtils.hasText(mjUserId) || !nodeJson.getString("userId").equals(mjUserId))) {
                        {
                            ((JSONObject) node).put("prompt", "🔒");
                            ((JSONObject) node).put("promptEn", "🔒");
                            ((JSONObject) node).put("promptFull", "🔒");
                            ((JSONObject) node).put("thumbnailUrl", "🔒");
                            ((JSONObject) node).put("imageUrl", "🔒");
                            ((JSONObject) node).put("description", "🔒");
                            ((JSONObject) node).put("nonce", "🔒");
                            ((JSONObject) node).put("jobId", "🔒");
                            ((JSONObject) node).put("instanceId", "🔒");
                            ((JSONObject) node).put("clientIp", "🔒");
                            ((JSONObject) node).put("userId", "🔒");
                        }
                    }
                });
                taskResponse.sort((a, b) -> {
                    if (a instanceof JSONObject && b instanceof JSONObject) {
                        if (!((JSONObject) a).containsKey("submitTime") || ((JSONObject) a).getLong("submitTime") == null) {
                            return -1;
                        }
                        if (!((JSONObject) b).containsKey("submitTime") || ((JSONObject) b).getLong("submitTime") == null) {
                            return -1;
                        }
                        return -1 * ((JSONObject) a).getLong("submitTime").compareTo(((JSONObject) b).getLong("submitTime"));
                    }
                    return 0;
                });
                userResponse.setList(taskResponse);
                userResponse.setPagination(new Pagination(page,size,(int)tasks.getTotal()));
                return HttpResult.success(userResponse);
            }
            return HttpResult.error("获取任务列表失败");
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return HttpResult.error("获取用户列表失败");
        }
    }


    @RequestMapping("/**")
    public HttpResult<JSONObject> proxyRequest(HttpServletRequest request, @RequestBody(required = false) String body) {
        String token = jwtTokenUtil.getTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            return HttpResult.error("用户未登录，请尝试刷新页面");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请联系管理员");
        }
        String secret = StringUtils.hasText(user.getMjProxyKey()) ? user.getMjProxyKey() : mjKey;
        String path = request.getRequestURI();
        //path = path.replaceFirst("^/mj", "");
        String targetUrl = (StringUtils.hasText(user.getMjProxyUrl()) ? user.getMjProxyUrl() : mjUrl) + path;

        // 获取请求方法（GET、POST 等）
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        // 设置请求头
        HttpEntity<String> entity = new HttpEntity<>(body, extractHeaders(request, secret));

        // 转发请求
        ResponseEntity<String> response = restTemplate.exchange(
                targetUrl,
                method,
                entity,
                String.class
        );

        JSONObject result = JSONObject.parseObject(response.getBody());
        if ((path.contains("imagine") || path.contains("modal") || path.contains("action")) && (result != null && result.containsKey("result"))) {
            List<MidjourneyTask> tasks = midjourneyTaskService.list(new LambdaQueryWrapper<MidjourneyTask>()
                                                                            .eq(MidjourneyTask::getUsername, user.getUniqueName())
                                                                            .eq(MidjourneyTask::getTaskId, result.getString("result")));
            if (CollectionUtils.isEmpty(tasks)) {
                MidjourneyTask midjourneyTask = new MidjourneyTask();
                midjourneyTask.setUsername(user.getUniqueName());
                midjourneyTask.setTaskId(result.getString("result"));
                midjourneyTask.setCreateTime(LocalDateTime.now());
                midjourneyTaskService.save(midjourneyTask);
            }
        }

        return HttpResult.success(result);
    }

    // 从原始请求中提取请求头
    private HttpHeaders extractHeaders(HttpServletRequest request, String secret) {
        HttpHeaders headers = new HttpHeaders();
        // 只添加必要的请求头
        headers.set("accept", "application/json, text/plain, */*");
        headers.set("accept-language", "zh-CN");
        headers.set("content-type", "application/json");
        headers.set("mj-api-secret", secret);
        headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0");
        return headers;
    }


}
