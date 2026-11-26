package com.snakedrop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.snakedrop.platform.PrefsScoreStore
import com.snakedrop.platform.ScoreStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = PrefsScoreStore(applicationContext)
        setContent { SnakeDropApp(store) }
    }
}

@Composable
fun SnakeDropApp(store: ScoreStore) {
    var playing by remember { mutableStateOf(false) }
    if (playing) {
        GameScreen(store = store, onHome = { playing = false })
    } else {
        HomeScreen(best = store.bestScore, onPlay = { playing = true })
    }
}
