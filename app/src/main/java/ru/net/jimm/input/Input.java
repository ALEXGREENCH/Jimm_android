package ru.net.jimm.input;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import jimm.Jimm;
import jimm.Options;
import jimm.comm.StringUtils;
import jimm.modules.Emotions;
import jimm.modules.Templates;
import jimmui.model.chat.ChatModel;
import jimmui.view.chat.Chat;
import jimmui.view.smiles.SmilesContent;
import ru.net.jimm.JimmActivity;
import ru.net.jimm.R;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 21.02.13 17:08
 *
 * @author vladimir
 */
public class Input extends LinearLayout implements View.OnClickListener, View.OnLongClickListener {

    private EditText messageEditor;
    private volatile Chat owner;
    private static State state = new State(null);
    private int layout = 0;
    private boolean sendByEnter;

    public Input(Context context, AttributeSet attrs, int id) {
        super(context, attrs);
        Log.d("init", "Input");
        updateInput();
        setId(id);
    }

    public void updateInput() {
        Log.d("init", "Input: updateInput");
        boolean simple = Options.getBoolean(Options.OPTION_SIMPLE_INPUT);
        final int newLayout = simple ? R.layout.input_simple : R.layout.input;
        if (layout != newLayout) {
            JimmActivity activity = (JimmActivity) getContext();
            activity.post(() -> {
                layout = newLayout;
                init();
                requestLayout();
            });
        }
    }

    private void init() {
        Log.d("init", "Input: init");

        removeAllViewsInLayout();
        ((Activity) getContext())
                .getLayoutInflater()
                .inflate(layout, this, true);

        messageEditor = findViewById(R.id.messageText);
        messageEditor.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        ImageView smileButton = findViewById(R.id.input_smile_button);
        smileButton.setOnClickListener(this);
        smileButton.setOnLongClickListener(this);
        ImageView sendButton = findViewById(R.id.input_send_button);
        sendByEnter = (null == sendButton);
        if (null != sendButton) {
            sendButton.setOnClickListener(view -> send());
        }
        if (sendByEnter) {
            messageEditor.setImeOptions(EditorInfo.IME_ACTION_SEND);
            messageEditor.setOnEditorActionListener(enterListener);
        }
        messageEditor.addTextChangedListener(textWatcher);
    }

    @Override
    public void onClick(View view) {
        Emotions.selectEmotion((canvas, cmd) -> ((JimmActivity) getContext()).post(() -> {
            insert(" " + ((SmilesContent) canvas).getSelectedCode() + " ");
            showKeyboard();
        }));
    }

    @Override
    public boolean onLongClick(View view) {
        Templates.getInstance().showTemplatesList((canvas, cmd) -> ((JimmActivity) getContext()).post(() -> {
            insert(Templates.getInstance().getSelectedTemplate());
            showKeyboard();
        }));
        return true;
    }

    private void showKeyboard(View view) {
        Configuration conf = Resources.getSystem().getConfiguration();
        if (conf.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_NO) {
            InputMethodManager keyboard = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(view, InputMethodManager.SHOW_FORCED | InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void showKeyboard() {
        ((JimmActivity) getContext()).post(() -> messageEditor.requestFocus());
        ((JimmActivity) getContext()).post(() -> showKeyboard(messageEditor));
    }

    private void send() {
        hideKeyboard(messageEditor);
        if (Jimm.getJimm().getDisplay().getCurrentDisplay() != owner) return;
        String message = getText();
        ChatModel model = owner.getModel();
        if (!model.getContact().isSingleUserContact() && message.endsWith(", ")) {
            message = "";
        }
        if (!StringUtils.isEmpty(message)) {
            Jimm.getJimm().jimmModel.registerChat(model);
            model.getProtocol().sendMessage(model.getContact(), message, true);
        }
        resetText();
    }

    public void setText(final String text) {
        ((JimmActivity) getContext()).post(() -> {
            String t = null == text ? "" : text;
            if ((0 == t.length()) || !canAdd(t)) {
                messageEditor.setText(t);
                messageEditor.setSelection(t.length());
            } else {
                insert(t);
            }
        });
        showKeyboard();
    }

    public boolean canAdd(String what) {
        String text = getText();
        if (0 == text.length()) return false;
        // more then one comma
        if (text.indexOf(',') != text.lastIndexOf(',')) return true;
        // replace one post number to another
        if (what.startsWith("#") && !text.contains(" ")) return false;
        return !text.endsWith(", ");
    }

    public void resetOwner() {
        Log.d("init", "Input: resetOwner");
        this.owner = null;
    }

    public void setOwner(Chat owner) {
        if (this.owner != owner) {
            this.owner = owner;
            if (null != owner) {
                final State newState = state.is(owner.getModel()) ? state : new State(owner.getModel());
                state = newState;
                String name = newState.ownerChatModel.getContact().isSingleUserContact()
                        ? newState.ownerChatModel.getContact().getName()
                        : null;
                final String hint = (null == name)
                        ? getContext().getString(R.string.hint_message)
                        : getContext().getString(R.string.hint_message_to, name);
                ((JimmActivity) getContext()).post(() -> {
                    messageEditor.setHint(hint);
                    messageEditor.setText(newState.text);
                    messageEditor.setSelection(newState.text.length());
                });
            }
        }
    }

    public void resetText() {
        ((JimmActivity) getContext()).post(() -> {
            messageEditor.setText("");
            state.text = "";
        });
    }

    public String getText() {
        return messageEditor.getText().toString();
    }

    public boolean hasText() {
        return 0 < messageEditor.getText().length();
    }

    public void insert(String text) {
        int start = messageEditor.getSelectionStart();
        int end = messageEditor.getSelectionEnd();
        messageEditor.getText().replace(Math.min(start, end), Math.max(start, end),
                text, 0, text.length());
    }

    private boolean isDone(int actionId) {
        return (EditorInfo.IME_NULL == actionId)
                || (EditorInfo.IME_ACTION_DONE == actionId)
                || (EditorInfo.IME_ACTION_SEND == actionId);
    }

    private final TextView.OnEditorActionListener enterListener = (exampleView, actionId, event) -> {
        if (isDone(actionId)) {
            if ((null == event) || (event.getAction() == KeyEvent.ACTION_DOWN)) {
                send();
                return true;
            }
        }
        return false;
    };

    private final TextWatcher textWatcher = new TextWatcher() {
        private String previousText;
        private int lineCount = 0;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            if (sendByEnter) {
                previousText = s.toString();
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (sendByEnter && (start + count <= s.length()) && (1 == count)) {
                boolean enter = ('\n' == s.charAt(start));
                if (enter) {
                    messageEditor.setText(previousText);
                    messageEditor.setSelection(start);
                    send();
                } else {
                    state.text = s;
                }
            } else {
                state.text = s;
            }
            if (lineCount != messageEditor.getLineCount()) {
                lineCount = messageEditor.getLineCount();
                messageEditor.requestLayout();
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };

    public Chat getOwner() {
        return owner;
    }

    private static class State {
        private final ChatModel ownerChatModel;
        private CharSequence text;

        public State(ChatModel model) {
            this.ownerChatModel = model;
            text = "";
        }

        public boolean is(ChatModel model) {
            return ownerChatModel == model;
        }
    }
}
