package com.autocast.app.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import com.autocast.app.MainActivity

class AutoCastMediaService : MediaBrowserServiceCompat() {

    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSessionCompat(this, "AutoCastMediaService").apply {
            setSessionActivity(pendingIntent)
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            val state = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_STOP
                )
                .setState(PlaybackStateCompat.STATE_PAUSED, 0L, 1.0f)
                .build()

            setPlaybackState(state)

            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    ScreenCastService.loadUrlInPresentation("https://m.youtube.com")
                }

                override fun onPause() {
                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                }

                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    when (mediaId) {
                        "youtube_mode" -> {
                            ScreenCastService.activeMode = ScreenCastService.MODE_YOUTUBE
                            updateMetadata("YouTube Widescreen Player", "Streaming YouTube HD")
                            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                            ScreenCastService.loadUrlInPresentation("https://m.youtube.com")
                        }
                        "mirror_mode" -> {
                            ScreenCastService.activeMode = ScreenCastService.MODE_SCREEN_CAST
                            updateMetadata("Full Device Screen Mirroring", "Mirroring Phone Screen & Audio")
                            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                        }
                        else -> {
                            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                        }
                    }
                }

                override fun onStop() {
                    updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                }
            })

            isActive = true
        }

        setSessionToken(mediaSession?.sessionToken)
    }

    private fun updateMetadata(title: String, subtitle: String) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, subtitle)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, subtitle)
            .build()
        mediaSession?.setMetadata(metadata)
    }

    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, 0L, 1.0f)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }

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
            .setSubtitle("Stream YouTube videos on car display")
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
