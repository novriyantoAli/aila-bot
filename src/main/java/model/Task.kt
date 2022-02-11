package model

import core.YoutubePlayer
import kotlinx.coroutines.Job

data class Task(
    val profileName: String,
    val job: Job,
    val driver: YoutubePlayer
)