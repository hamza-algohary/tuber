## TODO
### Capabilities 
- [ ] External plugins support
- [X] local list search
- [ ] ~~local list block~~ (delegate to frontend)
- [ ] ~~search merger~~ (delegated to frontend)
- [ ] SearchProvider rich info
- [ ] Additional Info to distinguish Videos/Movies/Series/Podcasts, etc.
- [ ] Ensure to clean summaries from dynamic data when storing and/or retrieving them from local lists.
- [ ] `all-MiniLM-L6-v2` is unsuitable for non english embeddings, replace with another one from same family
### URL Handlers 
- [ ] RSS
- [ ] M3U
- [ ] yt-dlp (external)

### Search Providers 
- [ ] duckduckgo
- [X] podcastindex

### End Points 
- [X] `lists`
- [X] `list add <list_name> <url>`
- [X] `list remove <list_name> <url>`
- [X] `list search <list_name> <query>`
- [X] `list export <name> # exports to Playlist JSON`
- [X] `list import <name> <path> # imports from a Playlist JSON`
- [ ] ~~`search [--blacklist=<list_name>] <args..>`~~ (delegate to frontend)
- [X] `podcasts update <path> # path tp podcastindex sqlite DB`
- [ ] `list-last-modification-time <list_name>` -> `Long` 
### Other
- [ ] Secret sauce
- [ ] Packaging and finalizing

### TODO LATER
- [ ] Turn all schemas into annotation+reflection based index.
- [ ] Also use functions+annotations to make CLIs too.
- [X] optimize `getAll()` of `Lists` and `DocumentsIndex`
- [X] Disallow duplicates
- [ ] Support caching entire channels listing , support periodic incremental sneaky update of cache with videos from whitelisted channels.
- [ ] Auto update mechanism without lists safety
- [ ] Auto update mechanism WITH lists safety
### Cancelled
- [ ] ~~Vector embeddings for podcastindex~~
- [ ] ~~Try different optimizations for embeddings like quantization, batching, embedding title only, etc.~~

### Notes
- MediaCCC is not working at all.


### Notes to be added to README to ensure proper use
1. Summary object may contain info not present in its corresponding info, so frontend is merge and both to get the full picture sometimes