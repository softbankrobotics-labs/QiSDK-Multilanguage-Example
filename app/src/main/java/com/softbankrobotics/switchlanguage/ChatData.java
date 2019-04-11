package com.softbankrobotics.switchlanguage;
import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.builder.QiChatbotBuilder;
import com.aldebaran.qi.sdk.builder.TopicBuilder;
import com.aldebaran.qi.sdk.object.conversation.AutonomousReactionImportance;
import com.aldebaran.qi.sdk.object.conversation.AutonomousReactionValidity;
import com.aldebaran.qi.sdk.object.conversation.Bookmark;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.EditablePhraseSet;
import com.aldebaran.qi.sdk.object.conversation.QiChatExecutor;
import com.aldebaran.qi.sdk.object.conversation.QiChatVariable;
import com.aldebaran.qi.sdk.object.conversation.QiChatbot;
import com.aldebaran.qi.sdk.object.conversation.Topic;
import com.aldebaran.qi.sdk.object.conversation.TopicStatus;
import com.aldebaran.qi.sdk.object.locale.Language;
import com.aldebaran.qi.sdk.object.locale.Region;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatData {

    private static final String TAG = "ChatData";
    private List<Topic> topics;
    public QiChatbot qiChatbot;
    public Chat chat;
    private Map<String, Map<String, Bookmark>> bookmarks;
    private Map<String, QiChatVariable> variables;
    private List<EditablePhraseSet> dynamics;
    private com.aldebaran.qi.sdk.object.locale.Locale qiLocale;
    private Future<Void> currentGotoBookmarkFuture;


    /**
     * Builds the main components of a QiChat, including a Chat, qiChatBot, the topics statues
     * and sets up for the Bookmarks.
     *
     * @param activity   the main activity where ChatData is built
     * @param qiContext  the activity qiContext
     * @param locale     the android locale you want to build the chat in
     * @param topicNames the names of the .top file you want to use in this chat bot
     * @param buildChat  if you are running the chat in the same activity that builds this object
     *                   this should be set to true, otherwise set it to false and use setupChat
     */
    public ChatData(Activity activity, QiContext qiContext, Locale locale,
                    List<String> topicNames, Boolean buildChat) {

        // Change locale for the time being, in order to get the right resource
        Resources res = activity.getApplicationContext().getResources();
        Configuration config = res.getConfiguration();
        Locale previousLocale = null; // needed if several languages are used.
        if (config.locale !=locale) {
            previousLocale = config.locale;
            config.setLocale(locale);
            res.updateConfiguration(config, res.getDisplayMetrics());
        }

        topics = new ArrayList<>();
        for (String topicName : topicNames) {
            Log.d(TAG, "adding " + topicName + " to topic list");
            topics.add(TopicBuilder.with(qiContext)
                    .withResource(getResId(topicName, R.raw.class))
                    .build());
        }

        qiLocale = getQiLocale(locale);

        qiChatbot = QiChatbotBuilder.with(qiContext)
                .withTopics(topics)
                .withLocale(qiLocale)
                .build();

        if (buildChat) {
            setupChat(qiContext);
        }

        bookmarks = new HashMap<>();
        for (Topic t : qiChatbot.getTopics()) {
            bookmarks.put(t.getName(), t.getBookmarks());
        }

        if (previousLocale != null) {
            config.setLocale(previousLocale);
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
    }

    /**
     * gets the resource ID thanks to its name and Class
     *
     * @param resName the resource name, as it appears in android studio
     * @param c       the class where this resource is located, for example if your file is a drawable
     *                this should be R.drawable.class
     * @return the resource ID
     */
    private static int getResId(String resName, Class<?> c) {
        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Translates the android Locale to a qiLocale if available, otherwise will return en_US
     *
     * @param locale the android locale
     * @return the qiLocale
     */
    private static com.aldebaran.qi.sdk.object.locale.Locale getQiLocale(Locale locale) {
        com.aldebaran.qi.sdk.object.locale.Locale qiLocale;
        String strLocale = locale.toString();
        if (strLocale.contains("fr")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.FRENCH, Region.FRANCE);
        } else if (strLocale.contains("zh")) {
            if (strLocale.equals("zh_CN")) {
                qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.CHINESE, Region.CHINA);
            } else {
                qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.CHINESE, Region.TAIWAN);
            }
        } else if (strLocale.contains("en")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.ENGLISH, Region.UNITED_STATES);
        } else if (strLocale.contains("ar")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.ARABIC, Region.EGYPT);
        } else if (strLocale.contains("da")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.DANISH, Region.DENMARK);
        } else if (strLocale.contains("nl")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.DUTCH, Region.NETHERLANDS);
        } else if (strLocale.contains("fi")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.FINNISH, Region.FINLAND);
        } else if (strLocale.contains("de")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.GERMAN, Region.GERMANY);
        } else if (strLocale.contains("it")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.ITALIAN, Region.ITALY);
        } else if (strLocale.contains("ja")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.JAPANESE, Region.JAPAN);
        } else if (strLocale.contains("nb")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.NORWEGIAN_BOKMAL, Region.NORWAY);
        } else if (strLocale.contains("es")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.SPANISH, Region.SPAIN);
        } else if (strLocale.contains("sv")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.SWEDISH, Region.SWEDEN);
        } else if (strLocale.contains("tr")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.TURKISH, Region.TURKEY);
        } else if (strLocale.contains("cs")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.CZECH, Region.CZECH_REPUBLIC);
        } else if (strLocale.contains("pl")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.POLISH, Region.POLAND);
        } else if (strLocale.contains("sk")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.SLOVAK, Region.SLOVAKIA);
        } else if (strLocale.contains("el")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.GREEK, Region.GREECE);
        } else if (strLocale.contains("ko")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.KOREAN, Region.REPUBLIC_OF_KOREA);
        } else if (strLocale.contains("hu")) {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.HUNGARIAN, Region.HUNGARY);
        } else {
            qiLocale = new com.aldebaran.qi.sdk.object.locale.Locale(Language.ENGLISH, Region.UNITED_STATES);
        }
        return qiLocale;
    }

    /**
     * sets up the chat, this function is separated from the constructor since you might want to
     * build ChatData in an activity that will not run the chat,
     * you can then build the chat in the activity where it is needed.
     *
     * @param qiContext the qiContext of the activity that is going to run the chat
     */

    public void setupChat(QiContext qiContext) {
        chat = ChatBuilder.with(qiContext)
                .withChatbot(qiChatbot)
                .withLocale(qiLocale)
                .build();
    }

    /**
     * sets up the qiVariables for this chat
     *
     * @param qiVariablesNames list of the names of the variables as they appear in qiChat
     */

    public void setupQiVariables(List<String> qiVariablesNames) {
        variables = new HashMap<>();
        for (String qiVariableName : qiVariablesNames) {
            variables.put(qiVariableName, qiChatbot.variable(qiVariableName));
        }
    }

    /**
     * sets up one qiVariable for this chat (if you need multiple variables use setupQiVariables)
     *
     * @param qiVariablesName the name of the variable
     */

    public void setupQiVariable(String qiVariablesName) {
        variables = new HashMap<>();
        variables.put(qiVariablesName, qiChatbot.variable(qiVariablesName));
    }

    /**
     * sets up the QiChatExecutors for this chat
     *
     * @param executors a map containing the names of the executors and the classes it is used with
     */
    public void setupExecutors(Map<String, QiChatExecutor> executors) {
        qiChatbot.setExecutors(executors);
    }

    /**
     * sets up one QiChatExecutor for this chat (if you need multiple executors use setupExecutors)
     *
     * @param executorName the name of the executor
     * @param executor     the instance of the executor
     */
    public void setupExecutor(String executorName, QiChatExecutor executor) {
        Map<String, QiChatExecutor> executors = new HashMap<>();
        executors.put(executorName, executor);
        qiChatbot.setExecutors(executors);
    }

    /**
     * sets up the dynamics Concepts for this chat
     *
     * @param dynamicConceptsNames list of the names of the dynamics concepts as they appear in qiChat
     */
    public void setupDynamics(List<String> dynamicConceptsNames) {
        dynamics = new ArrayList<>();
        for (String dynamicVariable : dynamicConceptsNames) {
            dynamics.add(qiChatbot.dynamicConcept(dynamicVariable));
        }
    }

    /**
     * updates the value of the qiVariable
     *
     * @param variableName the name of the variable
     * @param value        the value that needs to be set
     */

    public void setQiVariable(String variableName, String value) {
        Log.d(TAG, "size va : " + variables.size());
        variables.get(variableName).async().setValue(value);
    }

    /**
     * Goes to the specified bookmark in the specified topic already enabled)
     *
     * @param bookmark the name of the bookmark
     * @param topic    the name of the topic
     */
    public void goToBookmark(String bookmark, String topic) {
        if (bookmark.isEmpty()) {
            Log.w(TAG, "bookmark cannot be empty");
            return;
        }

        Log.d(TAG, "going to bookmark " + bookmark + " in topic : " + topic);
        Map<String, Bookmark> tmp = bookmarks.get(topic);
        if (tmp == null) {
            Log.w(TAG, String.format("Could not find topic %s", topic));
            return;
        }
        cancelCurrentGotoBookmarkFuture().thenConsume(uselessFuture ->
                currentGotoBookmarkFuture = qiChatbot.async().goToBookmark(tmp.get(bookmark),
                        AutonomousReactionImportance.HIGH, AutonomousReactionValidity.IMMEDIATE));
    }

    private Future<Void> cancelCurrentGotoBookmarkFuture() {
        if (currentGotoBookmarkFuture == null) return Future.of(null);
        currentGotoBookmarkFuture.cancel(true);
        return currentGotoBookmarkFuture;
    }

    public void onRobotFocusLost() {
        try {
            qiChatbot.removeAllOnEndedListeners();
        } catch (Exception ex) {
            Log.d(TAG, "onRobotFocusLost: " + ex.toString());
        }

        try {
            chat.removeAllOnStartedListeners();
        } catch (Exception ex) {
            Log.d(TAG, "onRobotFocusLost: " + ex.toString());
        }

        try {
            chat.removeAllOnStartedListeners();
        } catch (Exception ex) {
            Log.d(TAG, "onRobotFocusLost: " + ex.toString());
        }
    }

    public Future<Void> runChat() {
        for(Topic topic: topics) {
            qiChatbot.topicStatus(topic).setEnabled(true);
        }
        return chat.async().run();
    }
}
