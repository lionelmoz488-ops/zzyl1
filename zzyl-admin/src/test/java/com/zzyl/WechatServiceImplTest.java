package com.zzyl;

import com.zzyl.nursing.service.WechatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class WechatServiceImplTest {
    @Autowired
    private WechatService wechatService;

    @Test
    public void testGetOpenId() {
        String openId = wechatService.getOpenId("0b1jTA0w3tICb53wae2w3PyvOB3jTA0Y");
        System.out.println(openId);
    }

    @Test
    public void testGetPhone() {
        String phone = wechatService.getPhone("076e762969e40c0598d25dccbf5d4490fe65148584d81f90c6bf45fcfaa1b341");
        System.out.println(phone);
    }
}
