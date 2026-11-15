# Download Performance Benchmarks

Use this procedure before changing vendored `bt/**` disk, tracker, or peer-selection behavior. Public
swarms are not reproducible, so benchmark primarily with a controlled test swarm and use an official
Ubuntu torrent only as a lawful WAN sanity check.

## Metrics

Start Torenta and query the Actuator metrics endpoint:

```bash
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/torenta.bittorrent.time.to.first.peer
curl http://localhost:8080/actuator/metrics/torenta.bittorrent.time.to.first.piece
curl http://localhost:8080/actuator/metrics/torenta.bittorrent.time.to.completion
```

The `torenta.bittorrent.*` meters expose aggregate counts, timings, active downloads, connected peers,
and current session byte totals. Download ids, magnet links, peer addresses, and media names are not
metric tags.

## Test matrix

Run every candidate against the same payload, seed set, JVM, storage, and network profile:

| Scenario | Variants |
| --- | --- |
| Concurrency | 1 and 3 simultaneous downloads |
| Storage | Fast local SSD and a constrained-disk profile |
| Restart | Cold start, warm manual restart, and unchanged partial payload |
| Network | Baseline, added latency, packet loss, and a slow peer |
| Discovery | Healthy tracker, unavailable tracker, and mixed supported/unsupported trackers |
| Failure | Termination during block write, piece verification, checkpoint, and final copy |

Run at least five repetitions per variant. Record median and p95:

- time to metadata, first peer, and first verified piece;
- steady payload throughput and total completion time;
- manual restart/recheck time;
- process memory, thread count, open sockets, and disk queue depth;
- hash failures, duplicate bytes, and completion-tail duration when those engine metrics become
  available.

## Gates

- Do not accept a throughput change that weakens SHA-1 verification or crash recovery.
- Reject unbounded growth in memory, threads, sockets, or queued disk operations.
- Merge vendored disk-I/O changes only when constrained-disk throughput improves materially and all
  crash-injection cases recover through verified rechecking.
- No trusted piece may bypass hashing after a changed file, malformed checkpoint, unclean restart,
  or incompatible record version.
- With port 6891 occupied, the configured tracker/extended-handshake port must equal the fallback
  socket that remains bound for incoming peers.
- Treat a public-swarm result as supporting evidence only, never as the primary comparison.
