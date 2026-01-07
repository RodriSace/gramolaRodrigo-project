# TODO: Fix Queue Reset Issue

## Steps to Complete
- [x] Expand the default playlist in QueueService.java to include approximately 12 songs by adding more track IDs to initializeRealDeezerPlaylist()
- [x] Modify getQueue() method to integrate paid songs into the playlist flow instead of prioritizing them
- [x] Adjust playNextSong() method to ensure sequential playback without resets after paid songs
- [x] Add better error handling for songs without preview URLs
- [x] Add /api/deezer/url endpoint for URL resolution
- [x] Improve error handling in frontend for failed URL resolution
- [x] Expand playlist to 16 songs to reduce reset perception
- [ ] Test the queue playback to ensure songs play sequentially
- [ ] Verify that paid songs are inserted without disrupting the playlist flow
- [ ] Check that the playlist loops correctly through all 16 songs
