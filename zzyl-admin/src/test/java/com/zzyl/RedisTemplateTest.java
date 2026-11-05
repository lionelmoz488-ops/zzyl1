package com.zzyl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RedisTemplateTest {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Test
    public void testRedisTemplate() {
        System.out.println(redisTemplate);
    }

    @Test
    public void testString() {
        redisTemplate.opsForValue().set("name","张三");
        System.out.println(redisTemplate.opsForValue().get("name"));

        //设置带有过期时间的key
        redisTemplate.opsForValue().set("token","qwer1234",20, TimeUnit.SECONDS);

        //setnx
       redisTemplate.opsForValue().setIfAbsent("lock","qwer1234");
       redisTemplate.opsForValue().setIfAbsent("lock","zxcv123");
    }

    @Test
    public void testHash() {
        redisTemplate.opsForHash().put("user","name","张三");
        redisTemplate.opsForHash().put("user","age","18");
        System.out.println(redisTemplate.opsForHash().get("user","name"));
        System.out.println(redisTemplate.opsForHash().get("user","age"));

        System.out.println(redisTemplate.opsForHash().keys("user"));

        System.out.println(redisTemplate.opsForHash().values("user"));

        redisTemplate.opsForHash().delete("user","name");
    }

    @Test
    public void testList() {
        redisTemplate.opsForList().leftPush("mylist","a");
        redisTemplate.opsForList().leftPushAll("mylist","b","c","d");

        System.out.println(redisTemplate.opsForList().range("mylist",0,-1));

        System.out.println(redisTemplate.opsForList().leftPop("mylist"));

        System.out.println(redisTemplate.opsForList().range("mylist",0,-1));

        System.out.println(redisTemplate.opsForList().rightPop("mylist"));

        System.out.println(redisTemplate.opsForList().range("mylist",0,-1));

        System.out.println(redisTemplate.opsForList().size("mylist"));


    }

    @Test
    public void testSet() {
        redisTemplate.opsForSet().add("myset1","a","b","c","d");
        redisTemplate.opsForSet().add("myset2","a","b","x","y");
        System.out.println(redisTemplate.opsForSet().members("myset1"));

        //scard
        System.out.println(redisTemplate.opsForSet().size("myset1"));

        //sinter
        System.out.println(redisTemplate.opsForSet().intersect("myset1","myset2"));

        //sunion
        System.out.println(redisTemplate.opsForSet().union("myset1","myset2"));
    }

    @Test
    public void testZSet() {
        //zadd
        redisTemplate.opsForZSet().add("myzset","a",1);
        redisTemplate.opsForZSet().add("myzset","b",2);
        redisTemplate.opsForZSet().add("myzset","c",3);

        //zrange
        System.out.println(redisTemplate.opsForZSet().range("myzset",0,-1));

        //zincrby
        redisTemplate.opsForZSet().incrementScore("myzset","a",10);

        //zrange
        System.out.println(redisTemplate.opsForZSet().range("myzset",0,-1));

        //zrem
        redisTemplate.opsForZSet().remove("myzset","b");
    }

    @Test
    public void testCommon() {
        System.out.println(redisTemplate.keys("*"));

        //exist
        System.out.println(redisTemplate.hasKey("name"));
        System.out.println(redisTemplate.hasKey("names"));

        //type
        System.out.println(redisTemplate.type("name"));
        System.out.println(redisTemplate.type("mylist"));

        //del
        redisTemplate.delete("name");
        redisTemplate.delete(List.of("myset1", "mylist"));
    }

}
