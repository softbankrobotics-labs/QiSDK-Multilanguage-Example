package com.softbankrobotics.switchlanguage;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends RobotActivity implements RobotLifecycleCallbacks {
    private final String TAG = "SwitchLanguage";

    private final List<String> topics = Arrays.asList("welcome","lexicon");
    private final List<Locale> locales = Arrays.asList(Locale.FRENCH, Locale.US);

    private Map<Locale, ChatData> chatDataList; // We'll store one ChatData per Language
    private Boolean chatBuilt;
    private Future<Void> runningChat;

    private Resources res;
    private android.content.res.Configuration conf;

    private RadioButton enButton;
    private RadioButton frButton;
    private ProgressBar spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QiSDK.register(this, this);

        chatDataList = new HashMap<>();
        chatBuilt = Boolean.FALSE;

        res = getApplicationContext().getResources();
        conf = res.getConfiguration();

        changeUILanguage(conf.locale);

        Log.d(TAG, "onCreate: current locale is: " + conf.locale.toString());
    }

    private void changeLanguage(Locale locale) {
        if (conf.locale != locale) {
            runOnUiThread(this::stateUIThinking);
            runOnUiThread(() -> changeUILanguage(locale));
            changeChatLanguage(locale);
        }
    }

    private void changeUILanguage(Locale localeToSet) {
        Log.d(TAG, "changeUILanguage: localeToSet: " + localeToSet);
        conf.setLocale(localeToSet);
        res.updateConfiguration(conf, res.getDisplayMetrics());

        setContentView(R.layout.activity_welcome);

        spinner = findViewById(R.id.spinner);
        enButton = findViewById(R.id.enButton);
        frButton = findViewById(R.id.frButton);

        // Change the language to English when checked.
        enButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) changeLanguage(Locale.US);
        });

        // Change the language to French when checked.
        frButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) changeLanguage(Locale.FRENCH);
        });
    }

    private void changeChatLanguage(Locale localeToSet) {
        if(!chatBuilt) return;
        ChatData chatData = chatDataList.get(localeToSet);
        if(chatData == null) {
            Log.w(TAG, String.format("changeChatLanguage : chatData not found in Locale %s", localeToSet));
            return;
        }
        if (runningChat != null && !runningChat.isDone()) {
            // Cancel the current discussion.
            runningChat.requestCancellation();
            // Run the Chat when the discussion stops.
            runningChat.thenConsume(ignored -> runningChat = chatData.runChat());
        } else {
            // If no current discussion, just run the Chat.
            runningChat = chatData.runChat();
        }
    }

    private void stateUIThinking() {
        spinner.setVisibility(View.VISIBLE);
        enButton.setEnabled(false);
        enButton.getBackground().setAlpha(80);
        frButton.setEnabled(false);
        frButton.getBackground().setAlpha(80);
    }

    private void stateUIReady() {
        if (conf.locale == Locale.FRENCH) {
            Log.d(TAG, "stateUIReady: French");
            frButton.setChecked(true);
            enButton.setEnabled(true);
            enButton.getBackground().setAlpha(255);
            frButton.getBackground().setAlpha(100);
        } else {
            Log.d(TAG, "stateUIReady: English");
            enButton.setChecked(true);
            frButton.setEnabled(true);
            frButton.getBackground().setAlpha(255);
            enButton.getBackground().setAlpha(100);

        }
        spinner.setVisibility(View.GONE);
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        Log.d(TAG, "onRobotFocusGained started");
        buildAllChats(qiContext); // Create all objects (Topic, QiChatBot, Chat)
        changeChatLanguage(conf.locale); // run the chat in the language
        Log.d(TAG, "onRobotFocusGained finished");
    }

    private void buildAllChats(QiContext qiContext) {
        Log.d(TAG, "buildAllChats started");
        for (Locale locale : locales) {
            try {
                Log.d(TAG, String.format("buildAllChats: building chat in %s", locale));
                ChatData chatData = chatDataList.get(locale);
                if (!chatBuilt || chatData == null)
                    chatData = new ChatData(this, qiContext, locale, topics, true);
                bindOnEndedChatListeners(chatData);
                bindOnStartedListeners(chatData);
                chatDataList.put(locale, chatData);
            } catch (Exception e) {
                Log.e(TAG, String.format("buildAllChats: Error building chat in %s: %s", locale, e.getMessage()));
            }
        }
        chatBuilt = true;
        Log.d(TAG, "buildAllChats: finished");
    }

    private void bindOnStartedListeners(ChatData chatData) {
        chatData.chat.addOnStartedListener(() -> {
            chatData.goToBookmark("start", "welcome"); // say the hello
            runOnUiThread(this::stateUIReady); // display buttons on the screen);
        });
    }

    private void bindOnEndedChatListeners(ChatData chatData) {
        // Add a consumer to the action execution.
        chatData.qiChatbot.addOnEndedListener(endReason -> {
            Log.d(TAG, String.format("onEndedListener: Chat ended with reason: %s", endReason));
            runningChat.requestCancellation();
            switch (endReason) {
                case "switch_to_english":
                    changeLanguage(Locale.US);
                    break;
                case "switch_to_french":
                    changeLanguage(Locale.FRENCH);
                    break;
            }
        });
    }

    @Override
    public void onRobotFocusLost() {
        for(ChatData chatData: chatDataList.values()) {
            chatData.onRobotFocusLost();
        }
    }

    @Override
    public void onRobotFocusRefused(String reason) {
    }
}
