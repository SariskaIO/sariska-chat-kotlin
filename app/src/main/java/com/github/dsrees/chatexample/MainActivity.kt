package com.github.dsrees.chatexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import org.phoenixframework.Channel
import org.phoenixframework.Socket
import okhttp3.*
import java.io.IOException



class MainActivity : AppCompatActivity() {

  companion object {
    const val TAG = "MainActivity"
  }
  private val messagesAdapter = MessagesAdapter()
  private val layoutManager = LinearLayoutManager(this)

  private val client = OkHttpClient()
  fun run(): Response {
      val formBody = FormBody.Builder().add("apiKey", "24926faa88ca145d7466c2e123aca790768002ff8faf338e29ca").build()
      val request = Request.Builder()
        .url("https://api.sariska.io/api/v1/misc/generate-token").post(formBody)
        .addHeader("Accept", "application/json")
        .build()
    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) throw IOException("Unexpected code ${response.code()}")
      return response
    }
  }
  private val params = hashMapOf("token" to "eyJhbGciOiJSUzI1NiIsImtpZCI6IjI0OTI2ZmFhODhjYTE0NWQ3NDY2YzJlMTIzYWNhNzkwNzY4MDAyZmY4ZmFmMzM4ZTI5Y2EiLCJ0eXAiOiJKV1QifQ.eyJjb250ZXh0Ijp7InVzZXIiOnsiaWQiOiJlZWR2d3I0ZiIsIm5hbWUiOiJyb3VnaF9ob3JzZSJ9LCJncm91cCI6ImlraHoxNWNjdTN4ZnUyNTJldnR1ZHMifSwic3ViIjoiMjAiLCJyb29tIjoib3Jtb3hndGxhaDIyZXBja255YmJraCIsImlhdCI6MTYxMzY0MDY1OSwiaXNzIjoic2FyaXNrYSIsImF1ZCI6Im1lZGlhX21lc3NhZ2luZ19zYXJpc2thIiwiZXhwIjoxNjEzNzI3MDU5fQ.JD8DBDO3ynBviKgZdIMs6imB2ZFrw2zSywpE7uBrchd3AFxcpydXoBQpoU-3HwGH19sVtZiIcyErbRBDITUYEExS8Klm9g_0QmLg4_1vnv0wXUGppT_pjm0dmMFTzMQ2zgmgVz4dXKi7kXCPuulmlw4NGms9gMgz1oFeQ0nz-cxy1KH11pmJjLCQHg2PK0vNW6pLfmodF5GpN1pRpFcqBcBitHfv2X-uGSyWViOTazq3XhsP3N-ZBfCg4EknLoVfOKGtYL7MpCVKPx4abG9FmsMh_CjrpyKWGrubWNnTnectWMgfO4g6TUL2tWqYUQph6ZM1DOyNBl9yfpJZbZTElg")
  private val socket = Socket("wss://api.sariska.io/api/v1/messaging/websocket", params)
  private val topic = "chat:Chat10Feb"

  // Use when connecting to local server
//  private val socket = Socket("ws://10.0.2.2:4000/socket/websocket")
//  private val topic = "rooms:lobby"


  private var lobbyChannel: Channel? = null

  private val username: String
    get() = username_input.text.toString()

  private val message: String
    get() = message_input.text.toString()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)


    layoutManager.stackFromEnd = true

    messages_recycler_view.layoutManager = layoutManager
    messages_recycler_view.adapter = messagesAdapter

    socket.onOpen {
      this.addText("Socket Opened")
      runOnUiThread { connect_button.text = "Disconnect" }
    }

    socket.onClose {
      this.addText("Socket Closed")
      runOnUiThread { connect_button.text = "Connect" }
    }

    socket.onError { throwable, response ->
      Log.e(TAG, "Socket Errored $response", throwable)
      this.addText("Socket Error")
    }

    socket.logger = {
      Log.d(TAG, "SOCKET $it")
    }


    connect_button.setOnClickListener {
      if (socket.isConnected) {
        this.disconnectAndLeave()
      } else {
        this.disconnectAndLeave()
        this.connectAndJoin()
      }
    }

    send_button.setOnClickListener { sendMessage() }
  }

  private fun sendMessage() {
    this.messagesAdapter.add(message)
    val payload = mapOf("content" to message)
    this.lobbyChannel?.push("new_message", payload)
        ?.receive("ok") { Log.d(TAG, "success $it") }
        ?.receive("error") { Log.d(TAG, "error $it") }

    message_input.text.clear()
  }

  private fun disconnectAndLeave() {
    // Be sure the leave the channel or call socket.remove(lobbyChannel)
    lobbyChannel?.leave()
    socket.disconnect { this.addText("Socket Disconnected") }
  }

  private fun connectAndJoin() {
    val channel = socket.channel(topic, mapOf("status" to "joining"))
    channel.on("join") {
      this.addText("You joined the room")
    }

    channel.on("new_message") {message ->
      val payload = message.payload
      val username = payload["user"] as? String
      val body = payload["content"]
      this.addText("$body")
    }

    this.lobbyChannel = channel
    channel
        .join()
        .receive("ok") {
          this.addText("Joined Channel")
        }
        .receive("error") {
          this.addText("Failed to join channel: ${it.payload}")
        }


    this.socket.connect()
  }

  private fun addText(message: String) {
    runOnUiThread {
      this.messagesAdapter.add(message)
      layoutManager.smoothScrollToPosition(messages_recycler_view, null, messagesAdapter.itemCount)
    }

  }

}
