package com.dogonfire.gods.api.event;

import com.dogonfire.gods.api.QuestInfo;

/**
 * An event about a god's quest. Like {@link GodEvent}, it has no handler list of its own, so Bukkit
 * refuses an {@code @EventHandler} for it; listen to {@link QuestStartEvent}, {@link
 * QuestCompleteEvent} and {@link QuestFailEvent}.
 */
public abstract class QuestEvent extends GodEvent {
  private final QuestInfo quest;

  protected QuestEvent(QuestInfo quest) {
    super(quest.god());
    this.quest = quest;
  }

  /** The quest, as it is at this event */
  public QuestInfo getQuest() {
    return this.quest;
  }
}
