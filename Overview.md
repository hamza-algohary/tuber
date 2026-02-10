### Features
1. Videos, Movies, Podcasts, Live Channels, etc.
2. Whitelisting

### Streaming App Architecture
- Frontend (Searcher + Catalog/Recommendations + Video Player and different URL Handlers)
- Backend (Search Aggregator + URL Handler + Locally indexed lists)
- Plugins (Search Providers + URL Handlers)

### End points
Go the help message

```bash
    Search Commands:
        search-providers                                -> List<String> 
        search <search provider> <query> [--filters <colon separated list>] [--sort <criteria>] -> SearchResult
        filters  <search provider>                      -> List<String>
        sort-options <search provider>                  -> List<String>
    Url Handlers:
        stream   <url>                                  -> StreamInfo
        playlist <url>                                  -> PlaylistInfo
        channel  <url>                                  -> ChannelInfo
    Page Tokens Handler
        more     <page token>                            -> Items
    Catalogs/Recommendations
        catalogs                                        -> List<String>
        catalog  <catalog provider>                     -> List<PlaylistInfo>
    Lists
        lists                              -> List<String> # available lists names
        list-add    <list name> <url>      # Adds url of stream, playlist or channel to list. Adding a playlist also adds its entire content.
        list-remove <list name> <url>      # Removes an item from list, removing a playlist removes all its content
        list-search <list name> <query>    # Search inside a list
        list-export <list name> <path>     # Export an entire list to a file (a text file of NULL delimited stream of Summary JSONs)
        list-import <list name> <path>     # Import an entire list from a file
        list-delete <list name>            # Deletes an entire list
        list-plugins <list name>          # Get all plugins used in a list
        list-channels <list name>          # Get all explicitly added channels to a list
    Others
        prepare-podcastindex <path to podcastindex sqlite db>  -> Progressive null-delimited stream of Progress objects each in Json format.
```

### Implementation Plugins
- NewPipeExtractor (Search + URL Handlers) (YouTube, SoundCloud, PeerTube + 2 other sites)
- PodcastIndex Search
- RSS URL Handler (Podcasts and others)
- M3U (IPTV)

### Capabilities
- Locally indexed lists (supports exact and fuzzy and semantic search)
  - Using Apache Lucene (Fuzzy Search + Vector Search)
  - Using DJL (Vector embedding model)
  - Used in PodcastIndex (without semantic search, IPTV and Whitelists)

### What is left
- Packaging 

### To Be Done Later
- Update Mechanism
- Plugins
  - Web Search
  - yt-dlp
  - 
