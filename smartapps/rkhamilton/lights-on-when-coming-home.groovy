/**
 *  Lights On When Coming Home
 *
 *  Copyright 2016 Ryan Hamilton
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Lights On When Coming Home",
    namespace: "rkhamilton",
    author: "Ryan Hamilton",
    description: "Take action when a door opens after you have been away",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	//section("Only trigger when the house is in this mode:") {
	//	input "modes", "mode", required:true, title: "Select a mode(s)", multiple:false
	//}
	section("Activate when this door opens:") {
		input "thesensor", "capability.contactSensor", required:true, title: "Sensor?"
		//input "thesensor", "capability.switch", required:true, title: "Test switch?"
	}
    section("Turn on this dimmer:") {
		input "thedimmer", "capability.switchLevel", required:true, title: "Which dimmer switch?"
        input "thelevel", "number", required:true, title: "How bright (0-100)?"
        input "minutesSinceEvent", "number", required:true, title: "If nobody has pushed this button for this many minutes"
	}   
    section("Turn on this light:") {
		input "theswitch", "capability.switch", required:true, title:"Which switch?"
		input "switchDuration", "number", required:true, title:"For this many minutes"
	}        
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(thesensor, "contact.open", contactDetectedHandler)
	//subscribe(thesensor, "switch.on", contactDetectedHandler)
}

def contactDetectedHandler(evt) {
    log.debug "contactDetectedHandler called: $evt"
    def dimmerCurrentState = thedimmer.currentState("switch")
    def switchCurrentState = theswitch.currentState("switch")
    log.debug "contactDetectedHandler current dimmer state: $dimmerCurrentState.value"
    log.debug "contactDetectedHandler current switch state: $switchCurrentState.value"

	// if in the wrong mode then do nothing
	// if (location.mode != modes) return
    
    // if not between the start and end time then do nothing
    //def currentTime = new Date();
    //log.debug "contactDetectedHandler is $currentTime between $fromTime and $toTime"
    //def timeToAct = timeOfDayIsBetween(fromTime, toTime, currentTime, location.timeZone)                 
    //log.debug "contactDetectedHandler is it timeToAct? $timeToAct"
	//if(!timeToAct) return
 
 	// get sunrise and sunset times
    def currentTime = new Date();
 	def sunriseSunset = getSunriseAndSunset(zipCode: "94403")
    def afterDark = (currentTime > sunriseSunset.sunset)
    log.debug "now: $currentTime sunset: $sunriseSunset.sunset sunrise: $sunriseSunset.sunrise after dark: $afterDark"
 	if(!afterDark) return
    
    // turn on the front porch light for 10 minutes if it's not already on
    if (switchCurrentState.value == "off") {
    	theswitch.on()
		log.debug "scheduling turnOffLight in $switchDuration minutes"
    	runIn(switchDuration*60,turnOffLight)
        }

	// we need to decide if someone is leaving the house or arriving. We'll do this by looking at the event history of the dimmer
	use (groovy.time.TimeCategory) {
		log.debug "time $minutesSinceEvent min ago ${currentTime - minutesSinceEvent.minutes}"
	    def recentEvents = thedimmer.eventsSince(currentTime - minutesSinceEvent.minutes)
    	log.debug "there were ${recentEvents.size()} dimmer switch events in the last 10 minutes"
        if (recentEvents.size() > 0) {
        	log.debug "leaving the house, so do not turn on light"
        	return
            }


        // turn on the dimmer and set it to the desired level if it is off. If it's on we don't want to change the brightness
        if (dimmerCurrentState.value == "off") {
            thedimmer.setLevel(thelevel)
            }
        }
}

def turnOffLight() {
	log.debug "turnOffLight turning off switch"
    theswitch.off()
    }
