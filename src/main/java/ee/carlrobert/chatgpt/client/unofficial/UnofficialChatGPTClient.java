package ee.carlrobert.chatgpt.client.unofficial;

import ee.carlrobert.chatgpt.client.ApiRequestDetails;
import ee.carlrobert.chatgpt.client.Client;
import ee.carlrobert.chatgpt.client.unofficial.response.Response;
import ee.carlrobert.chatgpt.ide.settings.SettingsState;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.ResponseInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class UnofficialChatGPTClient extends Client {

  private static UnofficialChatGPTClient instance;
  private static Response lastReceivedResponse;

  private UnofficialChatGPTClient() {
  }

  public static UnofficialChatGPTClient getInstance() {
    if (instance == null) {
      instance = new UnofficialChatGPTClient();
    }
    return instance;
  }

  public void clearPreviousSession() {
    lastReceivedResponse = null;
  }

  protected ApiRequestDetails getRequestDetails(String prompt) {
    var settings = SettingsState.getInstance();
    var payload = new HashMap<>(Map.of(
        "action", "next",
        "messages", List.of(Map.of(
            "id", UUID.randomUUID().toString(),
            "role", "user",
            "author", Map.of("role", "user"),
            "content", Map.of(
                "content_type", "text",
                "parts", List.of(prompt)
            )
        )),
        "model", "text-davinci-002-render-sha"
    ));

    if (lastReceivedResponse != null) {
      payload.put("conversation_id", lastReceivedResponse.getConversationId());
      payload.put("parent_message_id", lastReceivedResponse.getMessage().getId());
    } else {
      payload.put("parent_message_id", UUID.randomUUID().toString());
    }

    return new ApiRequestDetails(
        settings.reverseProxyUrl,
        payload,
        settings.accessToken);
  }

  protected BodySubscriber<Void> subscribe(
      ResponseInfo responseInfo,
      Consumer<String> onMessageReceived,
      Runnable onComplete) {
    if (responseInfo.statusCode() == 200) {
      return new UnofficialChatGPTSubscriber(
          onMessageReceived,
          response -> {
            lastReceivedResponse = response;
            onComplete.run();
          });
    } else {
      onMessageReceived.accept("Something went wrong. Please try again later.");
      onComplete.run();
      throw new RuntimeException();
    }
  }
}
