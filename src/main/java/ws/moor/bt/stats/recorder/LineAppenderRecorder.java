/*
 * BitThief - A Free Riding BitTorrent Client
 * Copyright (C) 2006 Patrick Moor <patrick@moor.ws>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */

package ws.moor.bt.stats.recorder;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class LineAppenderRecorder implements Recorder {

  private long lastTimeLogged = Long.MIN_VALUE;

  private long lastValueSeen = Long.MIN_VALUE;
  private long lastTimeSeen = Long.MIN_VALUE;

  private final LineAppender lineAppender;

  private final long minimalLogInterval;
  private static final int DEFAULT_LOG_INTERVAL = 15 * 1000;

  private static final Logger logger = LogManager.getLogger();

  public LineAppenderRecorder(LineAppender appender, long minLogInterval) {
    lineAppender = appender;
    minimalLogInterval = minLogInterval;
  }

  public LineAppenderRecorder(LineAppender appender) {
    this(appender, DEFAULT_LOG_INTERVAL);
  }

  public void record(long time, long value) {
    if (value == lastValueSeen) {
      return;
    }
    if (lastTimeSeen != lastTimeLogged) {
      if (lastTimeSeen + minimalLogInterval <= time) {
        log(lastTimeSeen, lastValueSeen);
      }
    }

    if (lastTimeLogged + minimalLogInterval <= time) {
      log(time, value);
    }

    lastTimeSeen = time;
    lastValueSeen = value;
  }

  private void log(long time, long v) {
    try {
      lineAppender.append(Long.toString(time) + "\t" + Long.toString(v));
      lastTimeLogged = time;
    } catch (IOException e) {
      logger.warn("could not append", e);
    }
  }
}
