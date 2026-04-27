package com.zzyl;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONUtil;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class HttpTest {
    @Test
    public void testGet() {
        String result = HttpUtil.get("https://www.baidu.com");
        System.out.println(result);
    }

    @Test
    public void testGetByParam() {
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("pageNum", 1);
        paramsMap.put("pageSize", 5);
        String result = HttpUtil.get("http://localhost:8080/nursing/project/list", paramsMap);
        System.out.println(result);
    }

    @Test
    public void testCreateGetRequest() {
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("pageNum", 1);
        paramsMap.put("pageSize", 5);
        HttpResponse response = HttpUtil.createRequest(Method.GET, "http://localhost:8080/nursing/project/list")
                .form(paramsMap)
                .header("authorization", "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImxvZ2luX3VzZXJfa2V5IjoiNmExZDlkYTAtMjQ1Yi00MDg5LWFjZDYtNjVjMjBmYTJjOGQzIn0.GjcSwR8o8p88PEk-TMJ89hV3y07XqCemk8v9dezk263azexnP2nvvsT0kHWjUS8XBoWt4CG_eDQpU48HH9qmuA")
                .execute();
        if (response.isOk()) {
            System.out.println(response.body());
        }
    }

    @Test
    public void testPost() {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", "护理项目测试");
        paramMap.put("orderNo", 1);
        paramMap.put("unit", "次");
        paramMap.put("price", 10.00);
        paramMap.put("image", "https://yjy-slwl-oss.oss-cn-hangzhou.aliyuncs.com/ae7cf766-fb7b-49ff-a73c-c86c25f280e1.png");
        paramMap.put("nursingRequirement", "无特殊要求");
        paramMap.put("status", 1);
        String result = HttpUtil.post("http://localhost:8080/nursing/project", JSONUtil.toJsonStr(paramMap));
        System.out.println(result);
    }

    @Test
    public void testCreatePost() {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", "护理项目测试");
        paramMap.put("orderNo", 1);
        paramMap.put("unit", "次");
        paramMap.put("price", 10.00);
        paramMap.put("image", "https://yjy-slwl-oss.oss-cn-hangzhou.aliyuncs.com/ae7cf766-fb7b-49ff-a73c-c86c25f280e1.png");
        paramMap.put("nursingRequirement", "无特殊要求");
        paramMap.put("status", 1);
        HttpResponse response = HttpUtil.createPost("http://localhost:8080/nursing/project")
                .body(JSONUtil.toJsonStr(paramMap))
                .header("authorization", "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImxvZ2luX3VzZXJfa2V5IjoiODkwY2E1MmQtM2Q1MC00MmM3LTg4YzctOTYzMzdmYmU1NWU4In0.BDEa-hYvT54y67rvn2_YI6PfSA0nVrqaqusXAQ1eNbop6VFo68mar09MnA9HUYvKmcukeU2KLN4Ooo8lD-F4gA")
                .execute();
        if (response.isOk()) {
            System.out.println(response.body());
        }
    }
}
