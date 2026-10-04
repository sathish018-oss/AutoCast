package com.autocast.app.service

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat

class AutoCastMediaService : MediaBrowserServiceCompat() {

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot("root", null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()

        val youtubeDescription = MediaDescriptionCompat.Builder()
            .setMediaId("youtube_mode")
            .setTitle("YouTube Widescreen Player")
            .setSubtitle("Stream YouTube videos on head unit display")
            .build()

        val mirrorDescription = MediaDescriptionCompat.Builder()
            .setMediaId("mirror_mode")
            .setTitle("Full Device Screen Mirroring")
            .setSubtitle("Mirror phone display & audio in real-time")
            .build()

        mediaItems.add(
            MediaBrowserCompat.MediaItem(
                youtubeDescription,
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        )
        mediaItems.add(
            MediaBrowserCompat.MediaItem(
                mirrorDescription,
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        )

        result.sendResult(mediaItems)
    }
}
