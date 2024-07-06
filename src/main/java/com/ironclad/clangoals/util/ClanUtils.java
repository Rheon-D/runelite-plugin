package com.ironclad.clangoals.util;

import net.runelite.api.Client;
import net.runelite.api.clan.ClanSettings;

public class ClanUtils
{
	// Confirm that the current character is
	// a member of the clan.
	public static boolean isMemberOfClan(Client client)
	{
		final ClanSettings clan = client.getClanSettings();

		if(clan == null) return false;
		var name = clan.getName();
		var self = clan.findMember(client.getLocalPlayer().getName());
		if(self == null) return false;

		System.out.printf("Clan name: %s\n Rank: %s\n", name, self.getRank().name());

		return !(clan == null || !clan.getName().equals("IronClad"));
	}
}
