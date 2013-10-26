/**
 *  Forgiving Security
 *
 *  Author: brian@bevey.org
 *  Date: 10/25/13
 *
 *  Arm a simple security system based on mode.  Has a grace period to allow an
 *  ever present lag in presence detection.
 */

preferences {
  section("Modes to arm alarm?") {
    input "secureMode", "mode", title: "Mode to arm alarm (Away?)"
  }

  section("Things to secure?") {
    input "contacts", "capability.contactSensor", title: "Contact Sensors", multiple: true, required: false
    input "motions",  "capability.motionSensor",  title: "Motion Sensors",  multiple: true, required: false
  }

  section("Alarms to go off?") {
    input "alarms", "capability.alarm",  title: "Which Alarms?",       multiple: true, required: false
    input "lights", "capability.switch", title: "Flash which lights?", multiple: true, required: false
  }

  section("Delay for presence lag?") {
    input name: "presenceDelay", type: "number", title: "Seconds (defaults to 15s)", required: false
  }

  section("Notifications") {
    input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required: false
    input "phone", "phone", title: "Send a Text Message?", required: false
  }

  section("Message interval (default to every message)") {
    input name: "messageDelay", type: "number", title: "How Long?", required: false
  }
}

def installed() {
  init()
}

def updated() {
  unsubscribe()
  init()
}

def init() {
  state.lastClosed = now()
  subscribe(contacts, "contact.open",  triggerAlarm)
  subscribe(motions,  "motion.motion", triggerAlarm)
}

def triggerAlarm(evt) {
  def presenceDelay = presenceDelay ?: 15

  state.lastTrigger = now()

  log.info("Triggering alarm")
  runIn(presenceDelay, "fireAlarm")
}

def fireAlarm() {
  if(location.mode == secureMode) {
    log.info("Alarm triggered and still in secure mode.")
    send("Alarm has been triggered!")
    lights?.on()
    alarms?.both()
  }

  else {
    log.info("Alarm triggered, but it looks like you were just coming home.  Ignoring.")
  }
}

private send(msg) {
  def delay = (messageDelay != null && messageDelay != "") ? messageDelay * 60 * 1000 : 0

  if(now() - delay > state.lastMessage) {
    state.lastMessage = now()
    if (sendPushMessage != "No") {
      log.debug("Sending push message.")
      sendPush(msg)
    }

    if (phone) {
      log.debug("Sending text message.")
      sendSms(phone, msg)
    }

    log.debug(msg)
  }

  else {
    log.info("Have a message to send, but user requested to not not get it.")
  }
}