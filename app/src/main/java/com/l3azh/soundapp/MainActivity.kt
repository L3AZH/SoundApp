package com.l3azh.soundapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.l3azh.soundapp.ui.theme.SoundAppTheme

class MainActivity : ComponentActivity() {

    lateinit var  recordService:RecordService
    var id:Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recordService = RecordService(this)
        setContent {
            SoundAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Greeting("Android")
                    Column(
                        modifier =  Modifier.fillMaxWidth(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Button(onClick = {
                            id  = recordService.createAndStartRecord()
                        }) {
                            Text(text = "RECORD")
                        }
                        Button(onClick = {
                            recordService.stopRecord(id)
                        }) {
                            Text(text = "STOP")
                        }
                        Button(onClick = {
                            recordService.playRecord(id)
                        }) {
                            Text(text = "PLAY")
                        }
                        Button(onClick = {
                            recordService.clearRecordInstance()
                        }) {
                            Text(text = "CLEAR DATA")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recordService.clearRecordInstance()
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SoundAppTheme {
        Greeting("Android")
    }
}