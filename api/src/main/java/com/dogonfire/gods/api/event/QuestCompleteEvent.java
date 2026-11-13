package com.dogonfire.gods.api.event;

import com.dogonfire.gods.api.QuestInfo;
import org.bukkit.event.HandlerList;

/**
 * The believers completed their god's quest (or another plugin completed it): the god is pleased
 * and has rewarded them, and the quest is removed right after this event.
 */
public class QuestCompleteEvent extends QuestEvent {
  private static final HandlerList HANDLERS = new HandlerList();

  public QuestCompleteEvent(QuestInfo quest) {
    super(quest);
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
