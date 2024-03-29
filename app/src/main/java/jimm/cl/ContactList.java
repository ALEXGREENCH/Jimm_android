/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/jimm/ContactList.java
 Version: 0.7.1m  Date: 21.04.2022
 Author(s): Manuel Linsmayer, Andreas Rossbacher, Artyomov Denis
 *******************************************************************************/

package jimm.cl;

import jimm.chat.message.Message;
import jimm.modules.*;
import jimmui.model.chat.ChatModel;
import jimmui.updater.MessageBuilder;
import jimmui.updater.RosterUpdater;
import jimmui.view.chat.Chat;
import jimmui.view.icons.Icon;
import jimmui.view.roster.*;
import jimm.*;
import jimm.forms.*;
import jimmui.view.menu.*;
import jimmui.view.base.*;
import jimmui.view.roster.items.ProtocolBranch;
import jimmui.view.roster.items.TreeNode;
import protocol.*;
import protocol.xmpp.*;
import protocol.ui.ContactMenu;
import protocol.ui.InfoFactory;

import java.util.Vector;


public final class ContactList implements ContactListListener {
    private final ProtocolMenu mainMenu = new ProtocolMenu(null, true);
    private VirtualContactList contactList;

    public ContactList() {
    }
    public void initUI() {
        contactList = new VirtualContactList();
        contactList.setCLListener(this);
    }

    public void updateCl() {
        updateModel();
        updateMainMenu();
    }

    public void activate() {
        contactList.update();
        contactList.showMain();
    }
    public void _setActiveContact(Contact c) {
        if (null != c) {
            contactList.setActiveContact(c);
        }
        contactList.getModel().setAlwaysVisibleNode(c);
    }
    public void activate(Contact c) {
        contactList.updateOfflineStatus();
        _setActiveContact(c);
        activate();
    }
    public void activateWithMsg(String message) {
        activate();
        new Popup(contactList, message).show();
    }

    public void startUp() {
        if (0 == Options.getAccountCount()) {
            updateUnreadMessageCount();
            contactList.update();
            updateMainMenu();
            mainMenu.setDefaultItemCode(ProtocolMenu.MENU_STATUS);
            Jimm.getJimm().getDisplay().pushWindow(contactList);
            Jimm.getJimm().getDisplay().pushWindow(mainMenu.getView());
            new AccountsForm().showAccountEditor(null);

        } else {
            activate();
            Jimm.getJimm().getChatUpdater().loadUnreadMessages();
            updateUnreadMessageCount();
        }
    }

    /* *********************************************************** */
    final static public int SORT_BY_STATUS = 0;
    final static public int SORT_BY_ONLINE = 1;
    final static public int SORT_BY_NAME   = 2;

    /* *********************************************************** */

    public VirtualContactList getManager() {
        return contactList;
    }
    public RosterUpdater getUpdater() {
        return contactList.getUpdater();
    }

    /**
     * Adds the given message to the message queue of the contact item
     * identified by the given UIN
     */
    public void setActiveContact(Contact contact) {
        boolean isShown = (Jimm.getJimm().getDisplay().getCurrentDisplay() == contactList);
        if (isShown && (0 == cursorLock)) {
            contactList.setActiveContact(contact);
        }
    }

    private int cursorLock = 0;
    public final void userActivity() {
        cursorLock = 4 /* * 250 = 1 sec */;
        // #sijapp cond.if modules_ABSENCE is "true" #
        AutoAbsence.instance.userActivity();
        // #sijapp cond.end #
    }
    public final void timerAction() {
        // #sijapp cond.if modules_ABSENCE is "true" #
        AutoAbsence.instance.updateTime();
        // #sijapp cond.end #
        if (0 < cursorLock) {
            cursorLock--;
        }
        Jimm.getJimm().jimmModel.saveRostersIfNeed();
    }

    public final void receivedMessage(Protocol protocol, Contact contact, Message message, boolean silent) {
        // Notify splash canvas
        if (Jimm.getJimm().isLocked()) {
            Jimm.getJimm().splash.messageAvailable();
        }
        if (!silent) {
            addMessageNotify(protocol, protocol.getChatModel(contact), contact, message);
            if (Options.getBoolean(Options.OPTION_SORT_UP_WITH_MSG)) {
                getUpdater().updateContact(protocol, protocol.getGroup(contact), contact);
            }
        }
        updateUnreadMessageCount();
    }

    private void addMessageNotify(Protocol p, ChatModel chat, Contact contact, Message message) {
        boolean isPersonal = contact.isSingleUserContact();
        boolean isBlog = p.isBlogBot(contact.getUserId());
        boolean isHuman = isBlog || chat.isHuman() || !contact.isSingleUserContact();
        if (p.isBot(contact)) {
            isHuman = false;
        }
        boolean isMention = false;
        // #sijapp cond.if protocols_JABBER is "true" #
        if (!isPersonal && !message.isOffline() && (contact instanceof XmppContact)) {
            String msg = message.getText();
            String myName = ((XmppServiceContact)contact).getMyName();
            // regexp: "^nick. "
            isPersonal = msg.startsWith(myName)
                    && msg.startsWith(" ", myName.length() + 1);
            isMention = MessageBuilder.isHighlight(msg, myName);
        }
        // #sijapp cond.end #

        boolean isPaused = Jimm.getJimm().isPaused() && Jimm.getJimm().phone.isCollapsible();
        // #sijapp cond.if target is "MIDP2" #
        if (isPaused && isPersonal && isHuman) {
            if (Options.getBoolean(Options.OPTION_BRING_UP)) {
                Jimm.getJimm().maximize(getOrCreateChat(chat));
                isPaused = false;
            }
        }
        // #sijapp cond.end #

        if (!isPaused && isHuman) {
            if (isPersonal) {
                setActiveContact(contact);
            }
            // #sijapp cond.if modules_LIGHT is "true" #
            //if (isPersonal || isMention) {
            //    CustomLight.setLightMode(CustomLight.ACTION_MESSAGE);
            //}
            // #sijapp cond.end#
        }

        // #sijapp cond.if modules_SOUND is "true" #
        // Offline messages don't play sound
        if (!message.isOffline()) {
            if (isPersonal) {
                if (contact.isSingleUserContact()
                        && contact.isAuth() && !contact.isTemp()
                        && message.isWakeUp()) {
                    playNotification(p, Notify.NOTIFY_ALARM);

                } else if (isBlog) {
                    playNotification(p, Notify.NOTIFY_BLOG);

                } else if (isHuman) {
                    playNotification(p, Notify.NOTIFY_MESSAGE);
                }

                // #sijapp cond.if protocols_JABBER is "true" #
            } else if (isMention) {
                playNotification(p, Notify.NOTIFY_MULTIMESSAGE);
                // #sijapp cond.end #
            }
        }
        // #sijapp cond.end#
    }

    public final void playNotification(Protocol p, int type) {
        // #sijapp cond.if modules_SOUND is "true" #
        if (!p.isAway(p.getProfile().statusIndex)
                || Options.getBoolean(Options.OPTION_NOTIFY_IN_AWAY)) {
            Notify.getSound().playSoundNotification(type);
        }
        // #sijapp cond.end #
    }

    public final void markMessages(Protocol protocol, Contact contact) {
        if (null != contact) {
            if (Options.getBoolean(Options.OPTION_SORT_UP_WITH_MSG)) {
                getUpdater().updateContact(protocol, protocol.getGroup(contact), contact);
            }
        }
        if (null != MyActionBar.getMessageIcon()) {
            updateUnreadMessageCount();
        }
    }
    private void updateUnreadMessageCount() {
        Icon icon = getUnreadMessageIcon();
        if (icon != MyActionBar.getMessageIcon()) {
            MyActionBar.setMessageIcon(icon);
            Jimm.getJimm().getDisplay().getNativeCanvas().repaint();
        }
        // #sijapp cond.if modules_ANDROID is "true" #
        ru.net.jimm.JimmActivity.getInstance().service.updateAppIcon();
        // #sijapp cond.end #
    }

    public Icon getUnreadMessageIcon() {
        int icon = -1;
        Vector<ChatModel> chats = Jimm.getJimm().jimmModel.chats;
        for (int i = chats.size() - 1; 0 <= i; --i) {
            icon = InfoFactory.factory.getMoreImportant(icon, ((ChatModel)chats.elementAt(i)).getNewMessageIcon());
        }
        return InfoFactory.msgIcons.iconAt(icon);
    }
    public Icon getUnreadMessageIcon(Vector contacts) {
        int icon = -1;
        Contact c;
        for (int i = contacts.size() - 1; 0 <= i; --i) {
            c = (Contact)contacts.elementAt(i);
            icon = InfoFactory.factory.getMoreImportant(icon, c.getUnreadMessageIcon());
        }
        return InfoFactory.msgIcons.iconAt(icon);
    }
    public Icon getUnreadMessageIcon(Protocol p) {
        int icon = -1;
        ChatModel chat;
        Vector<ChatModel> chats = Jimm.getJimm().jimmModel.chats;
        for (int i = chats.size() - 1; 0 <= i; --i) {
            chat = (ChatModel)chats.elementAt(i);
            if (chat.getProtocol() == p) {
                icon = InfoFactory.factory.getMoreImportant(icon, chat.getNewMessageIcon());
            }
        }
        return InfoFactory.msgIcons.iconAt(icon);
    }

    /////////////////////////////////////////////////////////////////

    /** ************************************************************************* */

    /* Builds the main menu (visual list) */
    public void activateMainMenu() {
        if (contactList.getModel() == getUpdater().getChatModel()) {
            contactList.showMenu(new ChatMenu().getMenu());
        } else {
            updateMainMenu();
            mainMenu.setDefaultItemCode(ProtocolMenu.MENU_STATUS);
            mainMenu.getView().show();
        }
    }

    public void updateMainMenu() {
        mainMenu.updateMenu();
    }

    public final MenuModel getContextMenu(Protocol p, TreeNode node) {
        if (node instanceof Contact) {
            return new ContactMenu(p, (Contact) node).getContextMenu();
        }
        if (node instanceof Group) {
            if (p.isConnected()) {
                return new ManageContactListForm(p, (Group) node).getMenu();
            }
            return null;
        }
        if ((node instanceof ProtocolBranch) || (null == node)) {
            ProtocolMenu menu = new ProtocolMenu(p, false);
            menu.updateMenu();
            return menu.getModel();
        }
        return null;
    }

    public void updateModel() {
        contactList.setModel(contactList.getUpdater().createModel());
        contactList.getUpdater().addProtocols(Jimm.getJimm().jimmModel.protocols);
        contactList.updateOption();
    }

    public void typing(Protocol protocol, Contact item, boolean type) {
        // #sijapp cond.if modules_SOUND is "true" #
        if (type && protocol.isConnected()) {
            playNotification(protocol, Notify.NOTIFY_TYPING);
        }
        // #sijapp cond.end #

        getUpdater().typing(protocol, item);
    }

    public void setContactStatus(Protocol protocol, Contact contact, byte prev, byte curr) {
        if ((prev == curr) || !contact.isSingleUserContact()) {
            return;
        }
        // #sijapp cond.if protocols_JABBER is "true" #
        if (contact instanceof XmppServiceContact) {
            return;
        }
        // #sijapp cond.end #
        // #sijapp cond.if modules_SOUND is "true" #
        if (!protocol.isAway(curr) && protocol.isAway(prev)) {
            playNotification(protocol, Notify.NOTIFY_ONLINE);
        }
        // #sijapp cond.end #
        Jimm.getJimm().uiUpdater.showTopLine(protocol, contact, null, contact.getStatusIndex());
    }
    public void setContactStatus(Protocol protocol, Contact contact, String nick, byte status) {
        Jimm.getJimm().uiUpdater.showTopLine(protocol, contact, nick, status);
    }

    public void disconnected(Protocol protocol) {
        // #sijapp cond.if modules_SOUND is "true" #
        playNotification(protocol, Notify.NOTIFY_RECONNECT);
        // #sijapp cond.end #
    }

    //////////////////////////////////////////////////////////////////
    // chats

    public boolean isChats(Object canvas) {
        if (canvas instanceof VirtualContactList) {
            VirtualContactList vcl = ((VirtualContactList) canvas);
            return vcl.getModel() == vcl.getUpdater().getChatModel();
        }
        return false;
    }

    public void showChatList(boolean forceGoToChat) {
        ChatModel current = getPreferredChat();
        if (forceGoToChat && (null != current) && (0 < current.getUnreadMessageCount())) {
            Jimm.getJimm().getChatUpdater().activate(current);
            return;
        }
        getManager().setModel(getUpdater().getChatModel());
        if (null != current) {
            getManager().setActiveContact(current.getContact());
        }
        getManager().show();
    }

    public void backFromChats() {
        getManager().setModel(getUpdater().getModel());
        Jimm.getJimm().getDisplay().back(getManager());
    }

    // shows next or previous chat
    public void showNextPrevChat(ChatModel item, boolean next) {
        int chatNum = Jimm.getJimm().jimmModel.chats.indexOf(item);
        if (-1 == chatNum) {
            return;
        }
        int total = Jimm.getJimm().jimmModel.chats.size();
        int nextChatNum = (chatNum + (next ? 1 : -1) + total) % total;
        Vector<ChatModel> chats = Jimm.getJimm().jimmModel.chats;
        Jimm.getJimm().getChatUpdater().activate((ChatModel) chats.elementAt(nextChatNum));
    }

    private ChatModel getPreferredChat() {
        Vector<ChatModel> chats = Jimm.getJimm().jimmModel.chats;
        for (int i = 0; i < chats.size(); ++i) {
            ChatModel chat = (ChatModel) chats.elementAt(i);
            if (0 < chat.getPersonalUnreadMessageCount()) {
                return chat;
            }
        }
        Contact currentContact = getUpdater().getCurrentContact();
        int current  = 0;
        for (int i = 0; i < chats.size(); ++i) {
            ChatModel chat = (ChatModel) chats.elementAt(i);
            if (0 < chat.getUnreadMessageCount()) {
                return chat;
            }
            if (currentContact == chat.getContact()) {
                current = i;
            }
        }
        return current < chats.size() ? (ChatModel) chats.elementAt(current) : null;
    }

    public Chat getChat(ChatModel c) {
        if (null == c) return null;
        Object view = Jimm.getJimm().getDisplay().getCurrentDisplay();
        if (view instanceof Chat) {
            Chat chat = (Chat) view;
            if (chat.getModel() == c) {
                return chat;
            }
        }
        return null;
    }

    public Chat getOrCreateChat(ChatModel c) {
        Chat chat = getChat(c);
        if (null != c) {
            chat = new Chat(c);
        }
        return chat;
    }

    public void changeContactStatus(Protocol protocol, Contact contact) {
        ChatModel chat = Jimm.getJimm().jimmModel.getChatModel(contact);
        Chat view = getChat(chat);
        if (null != view) {
            view.updateStatus();
        }
        getUpdater().updateContact(protocol, protocol.getGroup(contact), contact);
    }
}