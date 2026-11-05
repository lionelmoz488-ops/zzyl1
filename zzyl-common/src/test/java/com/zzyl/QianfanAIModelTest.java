package com.zzyl;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ResponseFormatJsonObject;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

public class QianfanAIModelTest {

    public static void main(String[] args) {

        // 读取环境变量名，不能将 API Key 本身作为 getenv 的参数。
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        ;

// 创建 DeepSeek 客户端
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl("https://api.deepseek.com")
                .build();



        ChatCompletionCreateParams params =
                ChatCompletionCreateParams.builder()
                        .addUserMessage("你好")
                        .model("deepseek-flash")
                        .build();


        ChatCompletion chatCompletion = client.chat().completions().create(params);
        System.out.println(chatCompletion.choices().get(0).message().content().orElseGet(() -> ""));
    }
}