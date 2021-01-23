# JanaBot
A direct kotlin "simplementation" (at the moment) of https://github.com/elbekD/kt-telegram-bot

## Before you start
Check out https://core.telegram.org/bots.  
The code targets JVM 11, the current kotlin version is 1.4.20.

## Current features
- Subgroup functionality - do you have a big group chat that's muted by every member, 
and you'd sometimes like to tag a subset of your friends? Janabot has got you covered! 
Create a named subgroup, let people join, then tag the subgroup and janabot will @tag them,
notifying them of your message. Any number of subgroups is supported, as long as they have a unique name.  
There's one condition - everyone who wants to use this needs to have a nickname set in settings.

## Commands
The bot currently supports the following commands:
- /start - a simple greeting
- /help - a list of commands
- /g \<args> - functionality of subgroups within a chat group. Requires additional parameters:
    - \<group-name> - the bot will send a new message, tagging all the users in the given subgroup and telling
    them you are talking to them. 
    - \-create \<group-name> - creates a group with the defined name, unless one already exists in that chat room.
    - \-join \<group-name> - adds you to the group, unless you're already a member.
    - \-leave \<group-name> - removes you from the group.
    - \-delete \<group-name> - deletes the group. Only allowed to the person who originally created the group.
    - \-members \<group-name> - lists all members of the given group.
    - \-list - lists all groups in the given chat room.
    - \-listall - lists all groups the bot is maintaining at that moment, across all chat rooms.

## How to run:
1. Create your own bot user on telegram by following step 3 in https://core.telegram.org/bots.
2. Add the bot to desired chatrooms.
3. Run Main.kt locally with environment variables defining your bot's unique *username* and the *token* that was 
provided to you upon the bot's creation by The Botfather. Alternatively, package the code and deploy it to 
the code to host the bot remotely - check https://github.com/python-telegram-bot/python-telegram-bot/wiki/Where-to-host-Telegram-Bots for ideas.
4. That's it, your bot is now running. Type "/start" to greet him, or "/help" for a list of commands (currently very limited).