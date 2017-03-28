package fr.duniter.client.actions.utils;

/*
 * #%L
 * Duniter4j :: Client
 * %%
 * Copyright (C) 2014 - 2017 EIS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.beust.jcommander.internal.Maps;
import org.duniter.core.util.Preconditions;

import java.io.PrintStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by blavenie on 28/03/17.
 */
public class ClearableConsole extends PrintStream {

    public interface Color {
        int black = 30;
        int red = 31;
        int green = 32;
        int brown = 33;
        int blue = 34;
        int magenta = 35;
        int cyan = 36;
        int lightgray = 37;
    }

    private int rowsCount = 0;

    private Map<Pattern, Integer> regexColors = Maps.newHashMap();

    public ClearableConsole() {
        super(System.out, true);
    }

    public ClearableConsole(PrintStream delegate) {
        super(delegate, true);
    }

    public void clearConsole() {
        if (rowsCount == 0) {
            super.print("\033[H\033[2J");
            super.flush();
        }
        else {
            moveCursor(rowsCount);
        }
        rowsCount = 0;
    }

    public ClearableConsole putRegexColor(String regex, int color) {
        Preconditions.checkArgument(color >= 30 && color <= 37);

        regexColors.put(Pattern.compile(regex), color);
        return this;
    }

    public ClearableConsole removeRegex(String regex) {

        regexColors.remove(Pattern.compile(regex));
        return this;
    }

    public void moveCursor(int nbLinesUp) {
        for (int i = 1; i <= nbLinesUp; i++) {
            super.print("\033[1A"); // Move up
            super.print("\033[2K"); // Erase line content
        }

        super.flush();
    }

    @Override
    public void println(boolean x) {
        super.println(x);
        rowsCount++;
    }

    @Override
    public void println(char x) {
        super.println(x);
        rowsCount++;
    }

    @Override
    public void println(int x) {
        super.println(x);
        rowsCount++;
    }

    @Override
    public void println(long x) {
        super.println(x);
        rowsCount++;
    }

    @Override
    public void println(float x) {
        super.println(x);
        rowsCount++;
    }

    @Override
    public void println(double x) {
        super.println(x);
        rowsCount++;
    }

    @Override
    public void println(char[] x) {
        super.println(x);
        rowsCount++;
    }

    @Override
    public void println(String x) {

        super.println(x);
        rowsCount++;
    }

    @Override
    public void println(Object x) {
        super.println(x);
        rowsCount++;
    }

    @Override
    public void print(char c) {
        super.print(c);
    }

    @Override
    public void print(char[] x) {
        super.print(x);
        for (int i=0; i< x.length; i++) {
            if (x[i] == '\n') {
                rowsCount++;
            }
        }
    }

    @Override
    public void print(String s) {

        for (Pattern pattern: regexColors.keySet()) {
            Matcher matcher = pattern.matcher(s);
            if (matcher.find()) {
                StringBuilder sb = new StringBuilder();
                if (matcher.start() > 0) {
                    sb.append(s.substring(0, matcher.start()));
                }
                sb.append(String.format("\033[0;%sm", regexColors.get(pattern)));
                sb.append(s.substring(matcher.start(), matcher.end()));
                sb.append("\033[0m");
                if (matcher.end() < s.length()) {
                    sb.append(s.substring(matcher.end()));
                }
                s = sb.toString();
            }
        }
        super.print(s);
        for (int i=0; i< s.length(); i++) {
            if (s.charAt(i) == '\n') {
                rowsCount++;
            }
        }
    }

    @Override
    public void print(Object obj) {
        String s = String.valueOf(obj);
        print(s);
    }

    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        for (int i=start; i<=end; i++) {
            if (csq.charAt(i) == '\n') {
                rowsCount++;
            }
        }
        return super.append(csq, start, end);
    }

    @Override
    public PrintStream append(char c) {
        if (c == '\n') {
            rowsCount++;
        }
        return super.append(c);
    }
}
