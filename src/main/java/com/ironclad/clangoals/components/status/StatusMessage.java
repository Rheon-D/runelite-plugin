package com.ironclad.clangoals.components.status;

import lombok.Builder;
import lombok.Data;
import net.runelite.api.ChatMessageType;

@Data
@Builder
public class StatusMessage
{
	@Builder.Default
	ChatMessageType type = ChatMessageType.CLAN_MESSAGE;
	String sender;
	String value;
}
