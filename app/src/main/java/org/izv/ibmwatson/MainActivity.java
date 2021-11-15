package org.izv.ibmwatson;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.cloud.sdk.core.service.exception.NotFoundException;
import com.ibm.cloud.sdk.core.service.exception.RequestTooLargeException;
import com.ibm.cloud.sdk.core.service.exception.ServiceResponseException;
import com.ibm.watson.assistant.v2.Assistant;
import com.ibm.watson.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.assistant.v2.model.MessageInput;
import com.ibm.watson.assistant.v2.model.MessageOptions;
import com.ibm.watson.assistant.v2.model.MessageOutput;
import com.ibm.watson.assistant.v2.model.MessageResponse;
import com.ibm.watson.assistant.v2.model.RuntimeIntent;
import com.ibm.watson.assistant.v2.model.RuntimeResponseGeneric;
import com.ibm.watson.assistant.v2.model.SessionResponse;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    //https://hackernoon.com/mobile-api-security-techniques-682a5da4fe10

    private final String apikey = "cKI...cLL";
    private final String serviceUrl = "https://api.eu-de.assistant.watson.cloud.ibm.com/instances/bace5994-...8b8";
    private final String version = "2021-...-...";
    private final String assistantId = "0d9...1ef";

    private Assistant assistant;

    private TextView tvText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initalize();
    }

    private void initalize() {
        Button btWatson = findViewById(R.id.btWatson);
        EditText etText = findViewById(R.id.etText);
        tvText = findViewById(R.id.tvText);

        btWatson.setOnClickListener(view -> {
            if(!etText.getText().toString().isEmpty()) {
                sendMessageThread(etText.getText().toString());
            }
        });
    }

    private Assistant getAssistant() {
        IamAuthenticator.Builder builder = new IamAuthenticator.Builder();
        builder.apikey(apikey);
        IamAuthenticator authenticator = builder.build();
        Assistant assistant = new Assistant(version, authenticator);
        assistant.setServiceUrl(serviceUrl);
        return assistant;
    }

    private void sendMessageThread(String message) {
        new Thread() {
            @Override
            public void run() {
                if(assistant == null) {
                    assistant = getAssistant();
                }
                sendMessageBody(message);
            }
        }.start();
    }

    private void sendMessageBody(String message) {
        try {
            CreateSessionOptions options = new CreateSessionOptions.Builder(assistantId).build();
            SessionResponse response = assistant.createSession(options).execute().getResult();
            String sessionId = response.getSessionId();
            sendMessageText(message, sessionId);
        } catch (NotFoundException e) {
            showMessage(e.toString());
        } catch (RequestTooLargeException e) {
            showMessage(e.toString());
        } catch (ServiceResponseException e) {
            showMessage(e.toString() + "\n" + e.getStatusCode() + ": " + e.getMessage());
        }
    }

    private void sendMessageText(String message, String sessionId) {
        MessageInput input = new MessageInput.Builder()
                .messageType("text")
                .text(message)
                .build();
        MessageOptions options = new MessageOptions.Builder(assistantId, sessionId)
                .input(input)
                .build();
        MessageResponse response = assistant.message(options).execute().getResult();
        MessageOutput mo = response.getOutput();
        List<RuntimeIntent> intents = mo.getIntents();
        String text = "";
        for (RuntimeIntent ri: intents) {
            double confidence = ri.confidence();
            String intent = ri.intent();
            text += getString(R.string.intent) + ": " + intent + " (" + (confidence * 100) + " %)\n";
        }
        List<RuntimeResponseGeneric> generics = mo.getGeneric();
        for(RuntimeResponseGeneric rrg: generics) {
            text += getString(R.string.response) + ": " + rrg.text() + " (" + rrg.responseType() + ")\n";
        }
        showMessage(text);
    }

    private void showMessage(String message) {
        runOnUiThread(() -> {
            tvText.setText(message + tvText.getText().toString());
        });
    }
}