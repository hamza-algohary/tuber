## Tuber Developer Manual | How to use tuber as a media streaming app backend

## Introduction
**Tuber** is CLI tool that acts as a backend for media serving apps (something like YouTube)

Please read the full documentation **BEFORE** attempting to use, because there is a big chance you will miss very important information if you don't. (It is not long anyway)

## Features
Tuber implements 4 major features
1. Media search aggregator (plugins based, with some plugins builtin)
2. URL Handler for many sites (plugins based, with some plugins builtin) to allow proper display of channels and playlists, and play video and audio streams.
3. Unified schema for all the above, providing types like `Stream`, `Playlist` and `Channel` allowing frontend to transparently support new sites just by adding plugins. So frontend need not support individual sites.
4. Locally indexed lists, supporting fuzzy and semantic search, to aid frontends implement features such as IPTV, and Whitelists.

## Architecture
Tuber depends on Plugins (currently only internal, external support is on the way) to provide support for various sources.

A plugin may provide any number of these:
- Search Providers
- URL Handlers (Stream,Playlist,Channel)
- Catalog Providers (A catalog is some sort of unpersonalized generic suggested content, useful to display on first launch when you don't know what to show the user since you don't know his preference)

Currently available plugins:
- `NewPipeExtractor` (YouTube,SoundCloud,PeerTube search providers and URL handlers)
- `PodcastIndex` (take a look at <https://podcastindex.org>) (Podcasts Search Provider)
- `RSS` (Podcasts URL handler, any media in RSS format really)
- `M3U` (The chaotic format of IPTV lists)


## End Points
Run `tuber help` to all available endpoints and their return values. Do **NOT** use `--help` or rely on default help message as it is deficient at the moment.

## Examples 

## Guidelines, Examples and Suggestions
### The Schema
All the data types you will have to deal with are in [Schema.kt](app/src/main/kotlin/plugins/Schema.kt)

All objects are exchanged by JSON.

### The search life-cycle
Typically searching is done by using the following calls in order
1. `search-providers` to get available search providers (eg: YouTube,SoundCloud, etc.).
2. `filters <search provider>` to get filters provided by `search provider`.
3. `sort-options <search provider>` to get sort options provided by `search provider`.
4. `search <search provider> <query>` to search inside a specific search provider. Optionally using `--filters` to add a colon separated list of filters, and `--sort` to use a sort criteria (a single one)

### How and when to use lists features
Lists are optimized for searching (exact, or fuzzy or semantic). To make use of it you the following endpoints:
- `lists` to get available lists (should be empty initially)
- `list-add <list name> <url>` to add url to a list, if `list name` does not exist it will be created. A url may point to a stream (audio or video), playlist or channel. Any url that is supported through one of the plugins maybe used. When adding a playlist url, all its content is retrieved and put individually into the list, in addition to the playlist itself of course, that means that a search result may return a playlist, or one if its items separately. When adding a channel however, only the channel is added without any of its content.
- `list-remove <list name> <url>` remove `url` from `list name`
- `list-delete <list name>` delete entire list.
- `list-export <list name> <path>` export a entire list to a file. The file is null delimited sequence of `Summary` objects. It is null terminated to allow parsing it chunk by chunk wihtout loading the entire file into memory. Each object is in JSON format of course
- `list-import <list name> <path>` adds exported list at `path` into `list name`. Note: if list name already has some content it will **NOT** be discarded. If `list name` does not exist, it will be created.
- `list-search <list name> <query>` to search inside a list. 
- `list-services <list name>` all search providers, which have some content in `list name`. For example, if all URLs stored in `list name` are from YouTube, then it's going to return `["youtube"]`. If it has content from both SoundCloud and YouTube, then it is going to return `["youtube","soundcloud"]`.
- `list-channels <list name>` All channels that are explicitly stored in list. That excludes channels whose some of its content is added, without the channel itself being added.

`list-channels` and `list-services` are useful for implementing whitelists, go to whitelists section for more.
### How to properly update
You have to check current version using `version` and if it is less than latest available version in [GitHub release](https://github.com/hamza-algohary/tuber/releases) then you should update immediately (by simply discarding the old package and using the new one)

Before updating, however you should export all lists using `list-export` then delete them all using `list-delete` then update. Afterwards you should `list-import` all exported lists. This is crucial as the way we index lists is not stable, and may change without notice, what we will strive to stabilize however is endpoints and their resulted data schemas

### Policy to avoid outage in case a plugin breaks
What is likely to break is NewPipeExtractor (specifically one of YouTube features).
Don't worry as NewPipeExtractor developers quickly fix any breakage due to YouTube change, but that may take a day, and it's not good for your app to break for a day. Therefore, in case NewPipeExtractor (or any plugin for that matter) fails, use DuckDuckGo plugin (to be implemented) for search, and yt-dlp plugin (to be implemented) to handle URLs. As such you should definitely auto update yt-dlp as quickly as possible as well.

### Some suggestions on implementing the white-lists feature

### Properly handling live streams
For live streams you should rely on `hslUrl` or `dashUrl` fiels in `Info.Video` object. You may fall back to normal `videoStreams`,`videoOnlyStreams` and `audioStreams` only after trying hsl and dash URLs. For non-live videos you may do whatever.

### How to implement IPTV feature
An IPTV list is of the `M3U` format. It usually encodes a playlist of channels. You may add an `M3U` url to a list, then like any playlist all its content is going to be added, then you may search inside that list normally.

An IPTV `Playlist` will contain `Summary` objects of type `Summary.Generic`. If an item is a stream you should play it directly from the provided url. If it is a playlist, you may use `playlist` call.

IPTV playlists are chaotic, in the sense they may contain any url without restrictions.
### The `Items` class and how pagination is handled.
Here is the `Items` class:
```Kotlin
data class Items (
    val items : List<Summary>,
    val detailedItems : List<Info>,
    val nextPageToken : String?,
)
```
You will find an `Items` object in `SearchResult` and `Playlist`. 
An `Items` object may contain a list of `Summary` objects or list of `Info` or both. `Summary` type contains enough info just to display an item as part of search results, or playlist. As such to get more information about an item, you should throw its url at `stream`,`playlist` or `channel` commands, depending on Summary `type`. If `List<Info>` is provided you use it directly without any further handling.

### How to initialize PodcastIndex properly
Tuber does not ship with any data built in. In case of podcastindex you should download their sqlite database from [podcastindex.org](https://podcastindex.org/), then use the following special API to index the database.
`prepare-podcastindex <path to database>`
You rarely need to update this database, as what is indexed are the podcasts themselves not individual episodes, so you don't need to update often.

### Using `yt-dlp` properly (Not Yet Implemented)
You should update yt-dlp regularly, as soon as a new release is available.

### How build a rich video player around the available schema
TODO

### Display progress for long operations
Some operations return a stream of `Progress` objects, delimited by NULL characters.

```kotlin
data class Progress(val progress : Long , val total : Long)
```

Make use of this result to build a suitable progressbar.

Some long operations like `list-import` and `list-export` still have not implemented Progress reporting.