package com.ironclad.clangoals.components.tracking.npcs;

import com.google.gson.annotations.SerializedName;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import joptsimple.internal.Strings;
import lombok.Data;
import lombok.NonNull;

@Data
class MessageMatcher
{
	private static final String DEFAULT_TARGET = "name";
	/**
	 * The pattern to match the message against.
	 * Named groups are used to extract the target from the message.
	 */
	Pattern pattern;
	/**
	 * The target group to extract from the message.
	 * OR, the target string if targetIsGroup is false.
	 * Defaults to "name" if not provided.
	 * <p>
	 * The extracted value from the target group will be used to locate an {@link Mapping}
	 * If the target is not a group, the target will be used directly.
	 * For example, if the target is "Guardians of the Rift" and targetIsGroup is false,
	 * a Mapping with the name "Guardians of the Rift" will be used, if it exists.
	 */
	String target;
	@SerializedName("is_group")
	boolean targetIsGroup;

	private MessageMatcher(@NonNull Pattern pattern, @NonNull String target, boolean targetIsGroup)
	{
		this.pattern = pattern;
		this.target = target;
		this.targetIsGroup = targetIsGroup;
	}

	/**
	 * Apply the pattern to the message and return the target if found
	 *
	 * @param message the message to apply the pattern to
	 * @return Optional of the nullable target if found, empty otherwise
	 */
	@NonNull
	public Optional<String> apply(@NonNull String message)
	{
		Matcher m = this.pattern.matcher(message);
		if (m.find())
		{
			return Optional.ofNullable(this.targetIsGroup ? m.group(this.target) : this.target);
		}
		return Optional.empty();
	}

	public static final Predicate<MessageMatcher> VALIDATOR = matcher -> matcher != null && matcher.pattern != null && !Strings.isNullOrEmpty(matcher.target);
}
