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

package org.avrbuddy.log;

/**
 * @author Roman Elizarov
 */
public class Progress {
	private static final int HASHES = 50;

	private static Progress current;

	public static Progress start(String hrd, int total) {
		synchronized (Log.OUT) {
			if (current != null)
				throw new IllegalStateException();
			Progress progress = new Progress(hrd, total);
			current = progress;
			return progress;
		}
	}

	static void hide() {
		if (current != null)
			current.hideImpl();
	}

	static void restore() {
		if (current != null)
			current.showImpl();
	}

	// -------------------- instance --------------------

	private final StringBuilder sb = new StringBuilder();
	private final long startTime = System.currentTimeMillis();

	private final String hdr;
	private final int total;
	private int completed;
	private int percent;
	private double elapsed;

	private Progress(String hdr, int total) {
		this.hdr = hdr;
		this.total = total;
	}

	public void update(int completed) {
		synchronized (Log.OUT) {
			if (current != this)
				throw new IllegalStateException();
			this.completed = completed;
			int percent = 100 * completed / total;
			double elapsed = Math.round((System.currentTimeMillis() - startTime) / 100.0) / 10.0;
			if (this.percent != percent || this.elapsed != elapsed) {
				this.percent = percent;
				this.elapsed = elapsed;
				showImpl();
			}
		}
	}

	public void done() {
		synchronized (Log.OUT) {
			update(total);
			Log.OUT.println();
			current = null;
		}
	}

	private void showImpl() {
		sb.setLength(0);
		sb.append(hdr);
		sb.append(" [");
		int fillHash = percent * HASHES / 100;
		for (int i = 0; i < HASHES; i++)
			sb.append(i < fillHash ? '#' : '.');
		sb.append("] ");
		sb.append(completed);
		sb.append('/');
		sb.append(total);
		sb.append(" bytes (");
		sb.append(percent);
		sb.append("%) in ");
		sb.append(elapsed);
		sb.append("s");
		Log.OUT.print('\r');
		Log.OUT.print(sb);
		Log.OUT.flush();
	}

	private void hideImpl() {
		for (int i = 0; i < sb.length(); i++)
			sb.setCharAt(i, ' ');
		Log.OUT.print('\r');
		Log.OUT.print(sb);
		Log.OUT.print('\r');
		Log.OUT.flush();
	}
}


