package ch.andreskonrad.torenta.bittorrent.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Backend-computed action availability so the UI never has to infer safe actions itself. */
@AllArgsConstructor
@Getter
public class DownloadActionCapabilities {
    private final boolean canPause;
    private final boolean canRestart;
    private final boolean canStopAndDelete;
    private final boolean canRemove;
}
