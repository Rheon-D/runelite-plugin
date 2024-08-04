package com.ironclad.clangoals.util;

import net.runelite.api.Client;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;

public class ClanUtils
{
	/**
	 * Checks if the client is a member of the IronClad clan.
	 *
	 * @param client the client
	 * @return true if the client is a member of IronClad, false otherwise
	 */
	public static boolean isMemberOfClan(Client client)
	{
		ClanSettings clan = client.getClanSettings();
		if(clan == null) return false;
		ClanMember self = clan.findMember(client.getLocalPlayer().getName());
		if(self == null || self.getRank() == ClanRank.GUEST) return false;
		return clan.getName().equals("IronClad");
	}
}
