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
  section("Things to secure?") {
    input "contacts", "capability.contactSensor", title: "Contact Sensors", multiple: true, required: false
    input "motions",  "capability.motionSensor",  title: "Motion Sensors",  multiple: true, required: false
  }

  section("Alarms to go off?") {
    input "alarms", "capability.alarm",  title: "Which Alarms?",         multiple: true, required: false
    input "lights", "capability.switch", title: "Turn on which lights?", multiple: true, required: false
  }

  section("Delay for presence lag?") {
    input name: "presenceDelay", type: "number", title: "Seconds (defaults to 15s)", required: false
  }

  section("Notifications?") {
    input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required: false
    input "phone", "phone", title: "Send a Text Message?", required: false
  }

  section("Message interval?") {
    input name: "messageDelay", type: "number", title: "Minutes (default to every message)", required: false
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
  state.deviceTriggers = []
  state.lastClosed     = now()
  subscribe(contacts, "contact.open",  triggerAlarm)
  subscribe(motions,  "motion.active", triggerAlarm)
}

def triggerAlarm(evt) {
  def presenceDelay = presenceDelay ?: 15
  state.deviceTriggers.add(evt.displayName)
  state.triggerMode = location.mode
  state.lastTrigger = now()

  runIn(presenceDelay, "fireAlarm")
}

def fireAlarm() {
  if(state.deviceTriggers.size() > 0) {
    def devices = state.deviceTriggers.unique().join(", ")

    if(location.mode == state.triggerMode) {
      log.info(devices + " alarm triggered and mode hasn't changed.")
      send(devices + " alarm has been triggered!")
      lights?.on()
      alarms?.both()
    }

    else {
      log.info(devices + " alarm triggered, but it looks like you were just coming home.  Ignoring.")
    }
  }

  state.deviceTriggers = []
}

private send(msg) {
  def delay = (messageDelay != null && messageDelay != "") ? messageDelay * 60 * 1000 : 0

  if(now() - delay > state.lastMessage) {
    state.lastMessage = now()
    if(sendPushMessage == "Yes") {
      log.debug("Sending push message.")
      sendPush(msg)
    }

    if(phone) {
      log.debug("Sending text message.")
      sendSms(phone, msg)
    }

    log.debug(msg)
  }

  else {
    log.info("Have a message to send, but user requested to not get it.")
  }
}