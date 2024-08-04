package com.ironclad.clangoals.util.predicate;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ValidApiKey implements Predicate<String>
{
	private static final Pattern KEY_REGEX = Pattern.compile("^[a-zA-Z0-9]{20}$");
	@Override
	public boolean test(String s)
	{
		return s != null && s.length() == 20 && KEY_REGEX.matcher(s).matches();
	}
}
