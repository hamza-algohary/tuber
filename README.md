# Tuber

Tuber is a CLI frontend for YouTube, PeerTube, SoundCloud and other sites, powered by the fantastic [NewPipeExtractor] java library.

Moreover, tuber is suitable for usage as a backend since all its output is in JSON format.

# Usage as a server

### Available commands

On running `tuber help`:

```
commands:
    search-providers                                -> List<String>
    search   <search provider> <query> [filters]    -> SearchResult
    more     <pageToken>                            -> Items
    stream   <url>                                  -> StreamInfo
    playlist <url>                                  -> PlaylistInfo
    channel  <url>                                  -> ChannelInfo
    help
```

All output is in JSON. To know structure of resulted JSONs, please refer to [app/src/main/kotlin/backend/Backend.kt](app/src/main/kotlin/backend/Backend.kt)
