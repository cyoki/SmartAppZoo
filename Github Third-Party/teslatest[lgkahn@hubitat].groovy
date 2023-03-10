/**
 *  Tesla Connect
 *
 *  Copyright 2018 Trent Foley
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 * 10/18/20 lgkahn added open/unlock charge port
 * lgk added aption to put in new access token directly to get around login issues. Change4 to reset it to blank after use.
 * fix the updatesetting to clearsetting fx. 1/18/21
 *
 */

definition(
    name: "Tesla Connect 2.0",
    namespace: "trentfoley",
    author: "Trent Foley, Larry Kahn",
    description: "Integrate your Tesla car with SmartThings.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/tesla-app%402x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/tesla-app%403x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Partner/tesla-app%403x.png",
    singleInstance: true
)


    
preferences {
	page(name: "loginToTesla", title: "Tesla")
	page(name: "selectVehicles", title: "Tesla")
}

import java.security.MessageDigest

String.metaClass.encodeURL = {
   java.net.URLEncoder.encode(delegate, "UTF-8")
}

def giveMeKey(int length)
{
    String alphabet = (('A'..'N')+('P'..'Z')+('a'..'k')+('m'..'z')+('2'..'9')).join() 
     key = new Random().with {
           (1..length).collect { alphabet[ nextInt( alphabet.length() ) ] }.join()
             }
            return key
        }

public static String sha256(String base) {
    try{
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(base.getBytes("UTF-8"));
        StringBuffer hexString = new StringBuffer();

        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    } catch(Exception ex){
       throw new RuntimeException(ex);
    }
}

def loginToTesla() {
	def showUninstall = email != null && password != null
	return dynamicPage(name: "loginToTesla", title: "Connect your Tesla", nextPage:"selectVehicles", uninstall:showUninstall) {
		section("Log in to your Tesla account:") {
			input "email", "text", title: "Email", required: true, autoCorrect:false
			input "password", "password", title: "Password", required: true, autoCorrect:false
            input "newAccessToken", "string", title: "Input new access token when expired?", required: false
            input "refreshAccessTokenURL", "string", title: "URL (on your server) that holds new access token as generated from python script?", required: false         
		}
		section("To use Tesla, Hubitat encrypts and securely stores your Tesla credentials.") {}
	}
}

def selectVehicles() {
	try {
		refreshAccountVehicles()

		return dynamicPage(name: "selectVehicles", title: "Tesla", install:true, uninstall:true) {
			section("Select which Tesla to connect"){
				input(name: "selectedVehicles", type: "enum", required:false, multiple:true, options:state.accountVehicles)
			}
		}
	} catch (Exception e) {
    	log.error e
        return dynamicPage(name: "selectVehicles", title: "Tesla", install:false, uninstall:true, nextPage:"") {
			section("") {
				paragraph "Please check your username and password"
			}
		}
	}
}

def getChildNamespace() { "trentfoley" }
def getChildName() { "Tesla" }
def getServerUrl() { "https://owner-api.teslamotors.com" }
def getClientId () { "81527cff06843c8634fdc09e8ac0abefb46ac849f38fe1e431c2ef2106796384" }
def getClientSecret () { "c7257eb71a564034f9419ee651c7d0e5f7aa6bfbd18bafb5c5c033b093bb2fa3" }
def getUserAgent() { "trentacular" }


def getAccessToken() {
 
    if (newAccessToken)
    {
        log.debug "resetting access token"
        state.accessToken = newAccessToken
        app.clearSetting("newAccessToken")
    }
    else
    {
         if ((!state.accessToken) && (refreshAccessTokenURL))
        {
            log.debug "Attempting to get access token from url!"
            refreshAccessTokenfromURL()
        }   
       if ((!state.accessToken) && (!refreshAccessTokenURL))
        {
            
           getTokenFromLogin()
        }
    }
   
	state.accessToken
}

def getTokenFromLogin()

{
    log.debug "in Get token from login"
    def verifier = giveMeKey(86).toString()
    
   // log.debug "randomString = $newid"
    
  // def verifier = UUID.randomUUID().toString()
    
  
    
   if (!state.internalState)
    { 
        def internalState = UUID.randomUUID().toString()
        state.internalState = internalState
        log.info "Set internal state to $state.internalState"
    }
  else
  {
      log.info "Internal State = $state.internalState"
  }
    
   log.info " verifier = $verifier" 
   
   String challenge = sha256(verifier)
    def b64Challenge = challenge.encodeAsBase64()
 
    log.debug "before strip b4challenge = $b64Challenge"
    
    int loc = b64Challenge.indexOf("=")
    if (loc > 0)
    {
       log.debug "found loc = $loc stripping off ="
       b64Challenge = b64Challenge.substring(0,loc-1)
    }
        
    
    def urlChallenge = b64Challenge.encodeURL()
   
    log.info" challenge = $challenge"
    log.debug "b64 challenge = $b64Challenge"
    log.debug "url challenge = $urlChallenge"
    
    log.debug "client id = $clientId"
    
    // step 1 get the login page to get 
    
    /*
    
    
    def login(args):
    email, password = args.email, args.password
    session, resp, params, code_verifier = (None,) * 4
    vprint = print if args.verbose else lambda *_: None

CLIENT_ID = "81527cff06843c8634fdc09e8ac0abefb46ac849f38fe1e431c2ef2106796384"
UA = "Mozilla/5.0 (Linux; Android 10; Pixel 3 Build/QQ2A.200305.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/85.0.4183.81 Mobile Safari/537.36"
X_TESLA_USER_AGENT = "TeslaApp/3.10.9-433/adff2e065/android/10"


    headers = {
        "User-Agent": UA,
        "x-tesla-user-agent": X_TESLA_USER_AGENT,
        "X-Requested-With": "com.teslamotors.tesla",
    }

    # Step 1: Obtain the login page
    code_verifier, code_challenge, state = gen_params()

    params = (
        ("audience", ""),
        ("client_id", "ownerapi"),
        ("code_challenge", code_challenge),
        ("code_challenge_method", "S256"),
        ("locale", "en"),
        ("prompt", "login"),
        ("redirect_uri", "https://auth.tesla.com/void/callback"),
        ("response_type", "code"),
        ("scope", "openid email offline_access"),
        ("state", state),
    )

    session = requests.Session()
    resp = session.get("https://auth.tesla.com/oauth2/v3/authorize", headers=headers, params=params)
 
    
    httpPostJson([
                uri: serverUrl,
                path: "/oauth/token",
                headers: [ 'User-Agent': userAgent ],
                body: [
                    grant_type: "password",
                    client_id: clientId,
                    client_secret: clientSecret,
                    email: email,
                    password: password
                ]
        
        */
    
   
    
    state.cookiess = ''

    def params = [
                 uri: "https://auth.tesla.com",
                 path: "/oauth2/v3/authorize",
        headers: [ 
               'User-Agent' : "Mozilla/5.0 (Linux; Android 10; Pixel 3 Build/QQ2A.200305.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/85.0.4183.81 Mobile Safari/537.36",
               'x-tesla-user-agent': "TeslaApp/3.10.9-433/adff2e065/android/10",
               'X-Requested-With': "com.teslamotors.tesla"
            ],
        
           body:  [ audience: "",
                    client_id: "ownerapi",
                    code_challenge: urlChallenge,
                    code_challenge_method: "S256",
                    locale: "en",
                    prompt: "login",
                    redirect_url: "https://auth.tesla.com/void/callback",
                    response_type: "code",
                    scope: "openid email offlien_access",
                    state: state.internalState
                ]
        ]
    
   log.debug "Params: $params"
    
    try {
        httpPost(params) { 
            response ->
            log.debug "Request was successful, $response.status" // ${response.getHeaders()}"
            String allCookies = ""

            log.debug "response = $response.data"
            
       //     response.getHeaders('Set-Cookie').each { if (debugOutput) log.debug "---Set-Cookie: ${it.value}" }

            response.getHeaders('Set-Cookie').each {
                String cookie = it.value.split(';|,')[0]
                //log.debug "processing cookie = $it"
                Boolean skipCookie = false
                def expireParts = it.value.split('expires=')

                try {
                    def cookieSegments = it.value.split(';')
                    for (int i = 0; i < cookieSegments.length; i++) {
                        def cookieSegment = cookieSegments[i]
                        String cookieSegmentName = cookieSegment.split('=')[0]

                        if (cookieSegmentName.trim() == "expires") {
                            String expiration = cookieSegment.split('=')[1]

                            Date expires = new Date(expiration)
                            Date newDate = new Date() // right now

                            if (expires < newDate) {
                                skipCookie = true
                                //if (debugOutput) log.debug "-skip cookie: $it.value"
                            } else {
                                log.debug "+not skipping cookie: expires=$expires. now=$newDate. cookie: $it.value"
                            }

                        }
                    }
                } catch (e) {
                    log.debug "!error when checking expiration date: $e ($expiration) [$expireParts.length] {$it.value}"
                }

                allCookies = allCookies + it.value + ';'

                if (cookie != ".ASPXAUTH_TH_A=") {
                    if (it.value.split('=')[1].trim() != "") {
                        if (!skipCookie) {
                           log.debug "Adding cookie to collection: $cookie"
                            state.cookiess = state.cookiess + cookie + ';'
                        }
                    }
                }
            }
            log.debug "cookies: $state.cookiess"
        }
    } 
    catch (e) {
        log.warn "Something went wrong during login: $e"
       
    }
    }



def handleLoginResponse(resp) {
    log.debug "in handle login resp"
    def respCode = resp.getStatus()
    def rdata = resp.getData()
    log.debug "respose = $rdata"
    log.debug "status = $respCode"
    
}
private convertEpochSecondsToDate(epoch) {
	return new Date((long)epoch * 1000);
}

def refreshAccessTokenfromURL() {
    
        def params = [
        uri: refreshAccessTokenURL,
        headers: [
            'Accept': '*/*', // */ comment
            'cache': 'false',
            'Accept-Encoding': 'plain',
            'Accept-Language': 'en-US,en,q=0.8',
            'Connection': 'keep-alive',
            'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36'
        ],
	  timeout: 20
    ]
    log.debug "sending refresh request: $params"
    try {
          httpPostJson(params) { resp ->  
              log.debug "got data = $resp.data status $resp.status"
         
                    state.accessToken = resp.data.access_token
                    state.refreshToken = resp.data.refresh_token
                }
 }
    
    catch (groovyx.net.http.HttpResponseException e) {
            	log.warn e
                state.accessToken = null
                if (e.response?.data?.status?.code == 14) {
                    state.refreshToken = null
                }
    }
         
    }

             
def refreshAccessToken() {
	log.debug "refreshAccessToken"
	try {
        if (state.refreshToken) {
        	log.debug "Found refresh token so attempting an oAuth refresh"
            try {
                httpPostJson([
                    uri: serverUrl,
                    path: "/oauth/token",
                    headers: [ 'User-Agent': userAgent ],
                    body: [
                        grant_type: "refresh_token",
                        client_id: clientId,
                        client_secret: clientSecret,
                        refresh_token: state.refreshToken
                    ]
                ]) { resp ->
                    state.accessToken = resp.data.access_token
                    state.refreshToken = resp.data.refresh_token
                }
            } catch (groovyx.net.http.HttpResponseException e) {
            	log.warn e
                state.accessToken = null
                if (e.response?.data?.status?.code == 14) {
                    state.refreshToken = null
                }
            }
        }

       /* if (!state.accessToken) {
        	log.debug "Attemtping to get access token using password" 
            httpPostJson([
                uri: serverUrl,
                path: "/oauth/token",
                headers: [ 'User-Agent': userAgent ],
                body: [
                    grant_type: "password",
                    client_id: clientId,
                    client_secret: clientSecret,
                    email: email,
                    password: password
                ]
            ]) { resp ->
            	log.debug "Received access token that will expire on ${convertEpochSecondsToDate(resp.data.created_at + resp.data.expires_in)}"
                state.accessToken = resp.data.access_token
                state.refreshToken = resp.data.refresh_token
            }
        }
*/
    } catch (Exception e) {
        log.error "Unhandled exception in refreshAccessToken: $e response = $resp"
    }

        // above is deprecated for now get using url
       if ((!state.accessToken) && (refreshAccessTokenURL))
        {
            log.debug "Attempting to get access token from url!"
            refreshAccessTokenfromURL()
        }    
        
        
}

private authorizedHttpRequest(Map options = [:], String path, String method, Closure closure) {
    def attempt = options.attempt ?: 0
    
    log.debug "authorizedHttpRequest ${method} ${path} attempt ${attempt}"
    try {
    	def requestParameters = [
            uri: serverUrl,
            path: path,
            headers: [
                'User-Agent': userAgent,
                Authorization: "Bearer ${accessToken}"
            ]
        ]
    
    	if (method == "GET") {
            httpGet(requestParameters) { resp -> closure(resp) }
        } else if (method == "POST") {
        	if (options.body) {
            	requestParameters["body"] = options.body
                log.debug "authorizedHttpRequest body: ${options.body}"
                httpPostJson(requestParameters) { resp -> closure(resp) }
            } else {
        		httpPost(requestParameters) { resp -> closure(resp) }
            }
        } else {
        	log.error "Invalid method ${method}"
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        if (e.response?.data?.status?.code == 14) {
        	if (attempt < 3) {
                refreshAccessToken()
                authorizedHttpRequest(path, mehod, closure, body: options.body, attempt: attempt++)
            } else {
            	log.error "Failed after 3 attempts to perform request: ${path}"
            }
        } else {
        	log.error "Request failed for path: ${path}.  ${e.response?.data}"
        }
    }
}

private refreshAccountVehicles() {
	log.debug "refreshAccountVehicles"

	state.accountVehicles = [:]

	authorizedHttpRequest("/api/1/vehicles", "GET", { resp ->
    	log.debug "Found ${resp.data.response.size()} vehicles"
        resp.data.response.each { vehicle ->
        	log.debug "${vehicle.id}: ${vehicle.display_name}"
        	state.accountVehicles[vehicle.id] = vehicle.display_name
        }
    })
}


def installed() {
	log.debug "Installed"
	initialize()
}

def updated() {
	log.debug "Updated"

	unsubscribe()
	initialize()
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	log.debug "deleting ${delete.size()} vehicles"
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def initialize() {
    ensureDevicesForSelectedVehicles()
    removeNoLongerSelectedChildDevices()
}

private ensureDevicesForSelectedVehicles() {
	if (selectedVehicles) {
        selectedVehicles.each { dni ->
            def d = getChildDevice(dni)
            if(!d) {
                def vehicleName = state.accountVehicles[dni]
                device = addChildDevice("trentfoley", "Tesla", dni, null, [name:"Tesla ${dni}", label:vehicleName])
                log.debug "created device ${device.label} with id ${dni}"
                device.initialize()
            } else {
                log.debug "device for ${d.label} with id ${dni} already exists"
            }
        }
    }
}

private removeNoLongerSelectedChildDevices() {
	// Delete any that are no longer in settings
	def delete = getChildDevices().findAll { !selectedVehicles }
	removeChildDevices(delete)
}

private transformVehicleResponse(resp) {
	return [
        state: resp.data.response.state,
        motion: "inactive",
        speed: 0,
        vin: resp.data.response.vin,
        thermostatMode: "off"
    ]
}

def refresh(child) {
    def data = [:]
	def id = child.device.deviceNetworkId
    authorizedHttpRequest("/api/1/vehicles/${id}", "GET", { resp ->
        data = transformVehicleResponse(resp)
    })
    
    if (data.state == "online") {
    	authorizedHttpRequest("/api/1/vehicles/${id}/vehicle_data", "GET", { resp ->
            def driveState = resp.data.response.drive_state
            def chargeState = resp.data.response.charge_state
            def vehicleState = resp.data.response.vehicle_state
            def climateState = resp.data.response.climate_state
            
            data.speed = driveState.speed ? driveState.speed : 0
            data.motion = data.speed > 0 ? "active" : "inactive"            
            data.thermostatMode = climateState.is_climate_on ? "auto" : "off"
            
           
            data["chargeState"] = [
                battery: chargeState.battery_level,
                batteryRange: chargeState.battery_range,
                chargingState: chargeState.charging_state,
                minutes_to_full_charge: chargeState.minutes_to_full_charge    
            ]
            
            data["driveState"] = [
            	latitude: driveState.latitude,
                longitude: driveState.longitude,
                method: driveState.native_type,
                heading: driveState.heading,
                lastUpdateTime: convertEpochSecondsToDate(driveState.gps_as_of)
            ]
            
            data["vehicleState"] = [
            	presence: vehicleState.homelink_nearby ? "present" : "not present",
                lock: vehicleState.locked ? "locked" : "unlocked",
                odometer: vehicleState.odometer,
                sentry_mode: vehicleState.sentry_mode ? "On" : "Off",
                front_drivers_window: vehicleState.fd_window ? "Open" : "Closed",
                front_pass_window: vehicleState.fp_window ? "Open" : "Closed",
                rear_drivers_window: vehicleState.rd_window ? "Open" : "Closed",
                rear_pass_window: vehicleState.rp_window ? "Open" : "Closed"   
                ]

            
            data["climateState"] = [
            	temperature: celciusToFarenhiet(climateState.inside_temp),
                thermostatSetpoint: celciusToFarenhiet(climateState.driver_temp_setting),
                seat_heater_left: climateState.seat_heater_left,
                seat_heater_right: climateState.seat_heater_right, 
                seat_heater_rear_left: climateState.seat_heater_rear_left,  
                seat_heater_rear_right: climateState.seat_heater_rear_right,                
                seat_heater_rear_center: climateState.seat_heater_rear_center    
            ]
        })
    }
    
    return data
}

private celciusToFarenhiet(dC) {
	return dC * 9/5 + 32
}

private farenhietToCelcius(dF) {
	return (dF - 32) * 5/9
}

def wake(child) {
	def id = child.device.deviceNetworkId
    def data = [:]
    authorizedHttpRequest("/api/1/vehicles/${id}/wake_up", "POST", { resp ->
    	data = transformVehicleResponse(resp)
    })
    return data
}

private executeApiCommand(Map options = [:], child, String command) {
    def result = false
    authorizedHttpRequest(options, "/api/1/vehicles/${child.device.deviceNetworkId}/command/${command}", "POST", { resp ->
    	result = resp.data.result
    })
    return result
}

def lock(child) {
    wake(child)
    pause(2000)
	return executeApiCommand(child, "door_lock")
}

def unlock(child) {
    wake(child)
    pause(2000)
	return executeApiCommand(child, "door_unlock")
}

def unlockandOpenChargePort(child) {
    wake(child)
    pause(2000)
return executeApiCommand(child, "charge_port_door_open")
}

def climateAuto(child) {
    wake(child)
    pause(2000)
	return executeApiCommand(child, "auto_conditioning_start")
}

def climateOff(child) {
    wake(child)
    pause(2000)
	return executeApiCommand(child, "auto_conditioning_stop")
}

def setThermostatSetpointC(child, Number setpoint) {
	def setpointCelcius = setpoint.toInteger()
    wake(child)
    pause(2000)
    return executeApiCommand(child, "set_temps", body: [driver_temp: setpointCelcius, passenger_temp: setpointCelcius])
}


def setThermostatSetpointF(child, Number setpoint) {
	def setpointCelcius = farenhietToCelcius(setpoint).toInteger()
    wake(child)
    pause(2000)
    return executeApiCommand(child, "set_temps", body: [driver_temp: setpointCelcius, passenger_temp: setpointCelcius])
}

def startCharge(child) {
    wake(child)
    pause(2000)
	return executeApiCommand(child, "charge_start")
}

def stopCharge(child) {
   wake(child)
    pause(2000)
	return executeApiCommand(child, "charge_stop")
}

def openTrunk(child, whichTrunk) {
    wake(child)
    pause(2000)
	return executeApiCommand(child, "actuate_trunk", body: [which_trunk: whichTrunk])
}

def setSeatHeaters(child, seat,level) {
    wake(child)
    pause(2000)
	return executeApiCommand(child, "remote_seat_heater_request", body: [heater: seat, level: level])
}

def sentryModeOn(child) {
    wake(child)
    pause(2000)
	return executeApiCommand(child, "set_sentry_mode", body: [on: "true"])
}

def sentryModeOff(child) {
    wake(child)
    pause(2000)
	return executeApiCommand(child, "set_sentry_mode", body: [on: "false"])
}

def ventWindows(child) {
    wake(child)
    pause(2000)
	return executeApiCommand(child, "window_control", body: [command: "vent"])
}

def closeWindows(child) {
    wake(child)
    pause(2000)
	return executeApiCommand(child, "window_control", body: [command: "close"])
}

