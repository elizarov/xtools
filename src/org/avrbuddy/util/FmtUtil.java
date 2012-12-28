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

package org.avrbuddy.util;

import java.util.ArrayList;

/**
 * @author Roman Elizarov
 */
public class FmtUtil {
    public static final String SEP = "-";

    public static void line(ArrayList<String[]> table, String... s) {
        table.add(s);
    }

    public static String formatTable(ArrayList<String[]> table) {
        int n = table.size();
        int m = table.get(0).length;
        int[] width = new int[m];
        String[][][] s = new String[n][m][];
        for (int i = 0; i < n; i++) {
            String[] row = table.get(i);
            for (int j = 0; j < m; j++) {
                s[i][j] = row[j].split("\n");
                for (int k = 0; k < s[i][j].length; k++)
                    width[j] = Math.max(width[j], s[i][j][k].length());
            }
        }
        StringBuilder formatSb = new StringBuilder(" ");
        for (int j = 0; j < m; j++)
            formatSb.append(" %-").append(width[j]).append("s");
        String format = formatSb.toString();
        StringBuilder sb = new StringBuilder();
        String[] row = new String[m];
        for (int i = 0; i < n; i++) {
            int rc = 1;
            for (int j = 0; j < m; j++)
                rc = Math.max(rc, s[i][j].length);
            for (int k = 0; k < rc; k++) {
                if (sb.length() > 0)
                    sb.append('\n');
                for (int j = 0; j < m; j++)
                    row[j] = k < s[i][j].length ? s[i][j][k] : "";
                sb.append(String.format(format, row));
            }
        }
        return sb.toString();
    }
}
