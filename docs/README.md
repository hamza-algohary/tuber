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
Run `tuber help` to all available endpoints and their return values

## Examples 

## Guidelines, Examples and Suggestions
### The Schema
### The search life-cycle
Typically searching is done by using the following calls in order
1. `search-providers` to get available search providers (eg: YouTube,SoundCloud, etc.).
2. `filters <search provider>` to get filters provided by `search provider`.
3. `sort-options <search provider>` to get sort options provided by `search provider`.
4. `search <search provider> <query>` to search inside a specific search provider. Optionally using `--filters` to add a colon separated list of filters, and `--sort` to use a sort criteria (a single one)
### How and when to use lists features
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

### How pagination is handled
### How to initialize PodcastIndex properly
### Using yt-dlp properly (Not Yet Implemented)
### How build a rich video player around the available schema