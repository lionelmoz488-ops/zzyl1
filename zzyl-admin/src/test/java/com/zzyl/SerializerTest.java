package com.zzyl;

import com.zzyl.nursing.domain.NursingProject;
import com.zzyl.nursing.vo.NursingProjectVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;

@SpringBootTest
public class SerializerTest {

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Test
    public void testSerializer() {

        NursingProject np = new NursingProject();
        np.setId(1L);
        np.setName("测试");
        np.setOrderNo(1);
        np.setUnit("个");
        np.setPrice(new BigDecimal("10.00"));

        redisTemplate.opsForValue().set("nursingProject", np);

        NursingProject np1 = (NursingProject) redisTemplate.opsForValue().get("nursingProject");
        System.out.println(np1);

    }

    @Test
    public void testSerializer2() {

        NursingProjectVo npvo = new NursingProjectVo();

        npvo.setLabel("测试2");
        npvo.setValue("1");
        redisTemplate.opsForValue().set("nursingProjectVo", npvo);

        NursingProjectVo npvo1 = (NursingProjectVo) redisTemplate.opsForValue().get("nursingProjectVo");
        System.out.println(npvo1);

    }
}
