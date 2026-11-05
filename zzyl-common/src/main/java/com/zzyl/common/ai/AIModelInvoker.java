package com.zzyl.common.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ResponseFormatJsonObject;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AIModelInvoker {

    @Autowired
    private DSAIProperties dsAIProperties;
    public String DeepseekInvokerAI(String prompt) {

        ;


        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(dsAIProperties.getApiKey())
                .baseUrl(dsAIProperties.getBaseUrl())
                .build();



        ChatCompletionCreateParams params =
                ChatCompletionCreateParams.builder()
                        .addUserMessage(prompt)
                        .model(dsAIProperties.getModel())
                        .responseFormat(ChatCompletionCreateParams.ResponseFormat.ofJsonObject(ResponseFormatJsonObject.builder().build()))
                        .build();


        ChatCompletion chatCompletion = client.chat().completions().create(params);

        return chatCompletion.choices().get(0).message().content().orElseGet(() -> "");
    }
}
