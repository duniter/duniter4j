package org.duniter.client.actions.utils;

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
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.fusesource.jansi.Ansi.Erase;
import static org.fusesource.jansi.Ansi.ansi;

/**
 * Created by blavenie on 28/03/17.
 */
public class RegexAnsiConsole extends PrintStream {

    private Long rowsCount = 0l;
    private Map<Pattern, Ansi.Color> fgRegexps = Maps.newHashMap();

    public RegexAnsiConsole(OutputStream delegate) {
        super(AnsiConsole.wrapOutputStream(delegate), true);
    }

    public RegexAnsiConsole() {
        super(AnsiConsole.out(), true);
    }

    public RegexAnsiConsole eraseScreen() {
        Ansi ansi = ansi();
        synchronized (rowsCount) {
            // If first call to erase: clean screen then save cursor position
            if (rowsCount == 0) {
                ansi.eraseScreen()
                        .cursor(0,0)
                        .saveCursorPosition();
            }
            else {
                // Try to erase writtent lines
                for (int i = 1; i <= rowsCount; i++) {
                    ansi.cursorUp(1).eraseLine(Erase.ALL);
                }

                // Reset rowcount
                rowsCount = 0l;

                // Make sure to go back to saved cursor point
                // Then clean screen again
                ansi.restoreCursorPosition()
                        .eraseScreen();
            }

        }

        super.print(ansi);
        return this;
    }
    
    public RegexAnsiConsole resetFgRegexps() {
        fgRegexps.clear();
        return this;
    }

    public RegexAnsiConsole fgRegexp(String regex, Ansi.Color color) {
        fgRegexps.put(Pattern.compile(regex), color);
        return this;
    }

    @Override
    public void print(String s) {

        for (Pattern pattern: fgRegexps.keySet()) {
            Matcher matcher = pattern.matcher(s);
            if (matcher.find()) {
                Ansi.Color fgColor = fgRegexps.get(pattern);
                Ansi ansi = ansi();
                if (matcher.start() > 0) {
                    ansi.a(s.substring(0, matcher.start()));
                }

                ansi.fg(fgColor)
                        .a(s.substring(matcher.start(), matcher.end()))
                        .reset();
                if (matcher.end() < s.length()) {
                    ansi.a(s.substring(matcher.end()));
                }
                s = ansi.toString();
            }
        }

        long newLineCount = 0;
        for (int i=0; i<s.length(); i++) {
            if (s.charAt(i) == '\n') newLineCount++;
        }
        incRowCount(newLineCount);

        super.print(s);
    }

    @Override
    public void println(boolean x) {
        super.println(x);
        incRowCount();
    }

    @Override
    public void println(char x) {
        super.println(x);
        incRowCount();
    }

    @Override
    public void println(int x) {
        super.println(x);
        incRowCount();
    }

    @Override
    public void println(long x) {
        super.println(x);
        incRowCount();
    }

    @Override
    public void println(float x) {
        super.println(x);
        incRowCount();
    }

    @Override
    public void println(double x) {
        super.println(x);
        incRowCount();
    }

    @Override
    public void println(char[] x) {
        super.println(x);
        incRowCount();
    }

    @Override
    public void println(String x) {

        super.println(x);
        incRowCount();
    }

    @Override
    public void println(Object x) {
        super.println(x);
        incRowCount();
    }

    @Override
    public void print(char c) {
        super.print(c);
        if (c == '\n') incRowCount();
    }

    @Override
    public void print(char[] x) {
        print(new String(x));
    }

    @Override
    public void print(Object obj) {
        print(String.valueOf(obj));
    }

    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        long nbNewLine = csq.chars().filter(c -> c == '\n').count();
        incRowCount(nbNewLine);
        return super.append(csq, start, end);
    }

    @Override
    public PrintStream append(char c) {
        if (c == '\n') {
            incRowCount();
        }
        return super.append(c);
    }

    /* -- protected methods -- */

    protected void incRowCount() {
        synchronized (rowsCount) {
            rowsCount++;
        }
    }

    protected void  incRowCount(long increment) {
        synchronized (rowsCount) {
            rowsCount += increment;
        }
    }
}
