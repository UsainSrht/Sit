package com.purpurmc.sit;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Collection;
import java.util.HashSet;

public class StringArgument implements ArgumentType<String> {

    public final Collection<String> correct;

    public StringArgument(final Collection<String> correct) {
        this.correct = correct;
    }

    public static StringArgument string() {
        Collection<String> args = new HashSet<>();
        args.add("reload");
        return string(args);
    }

    public static StringArgument string(Collection<String> args) {
        return new StringArgument(args);
    }

    public static String getString(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String result = reader.readString();

        if (correct.contains(result)) {
            reader.setCursor(start);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect().createWithContext(reader, result);
        }
        return result;
    }
}
