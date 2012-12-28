/*
 * Copyright (C) 2012 Roman Elizarov
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.avrbuddy.xbee.cmd.impl;

import org.avrbuddy.avr.AvrOperation;
import org.avrbuddy.avr.AvrProgrammer;
import org.avrbuddy.xbee.cmd.Command;
import org.avrbuddy.xbee.cmd.CommandContext;

import java.io.IOException;
import java.util.EnumSet;

/**
 * @author Roman Elizarov
 */
public class Avr extends Command {
    private AvrOperation operation;

    @Override
    public EnumSet<Option> getOptions() {
        return EnumSet.of(Option.DEST, Option.DEST_REQUIRED, Option.ARG);
    }

    @Override
    public String getCommandDescription() {
        return "Performs specified operation with Arduino AVR bootloader.";
    }

    @Override
    public String getParameterDescription() {
        return "[<memop>]";
    }

    @Override
    public String getMoreHelp() {
        return "Where <memop> is <memtype>:<memcmd>:<filename>[:<format>] and\n" +
            "  <memtype> is 'f' or 'e'; <memcmd> is 'r', 'w', or 'v'; and the only supported <format> in 'i'.";
    }

    @Override
    public void validate(CommandContext ctx) {
        super.validate(ctx);
        if (getArg() != null)
            operation = AvrOperation.parse(getArg());
    }

    @Override
    protected String invoke(CommandContext ctx) throws IOException {
        AvrProgrammer pgm = AvrProgrammer.connect(ctx.conn.openTunnel(destination.resolveAddress(ctx)));
        if (operation != null)
            operation.execute(pgm);
        pgm.quit();
        pgm.close();
        return (operation != null ?  operation + " " : "") + OK;
    }
}
