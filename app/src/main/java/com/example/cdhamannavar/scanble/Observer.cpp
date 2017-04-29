//
// Created by cdhamannavar on 4/29/2017.
//

/* mbed Microcontroller Library
 * Copyright (c) 2006-2015 ARM Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "mbed.h"
//#include "toolchain.h"
#include "ble/BLE.h"
//#include "TMP_nrf51/TMP_nrf51.h"

const static char     DEVICE_NAME[]        = "BSL00LR1"; // change this
static const uint16_t uuid16_list[]        = {0xFFFF}; //Custom UUID, FFFF is reserved for development
uint8_t AdvData[5] = {0x00,0x00,0x00,0x00,0x00};

DigitalOut alivenessLED(LED1, 1);
Ticker     ticker;

void periodicCallback(void)
{
    alivenessLED = !alivenessLED; /* Do blinky on LED1 while we're waiting for BLE events. This is optional. */
}

/*
 * This function is called every time we scan an advertisement.
 */
void advertisementCallback(const Gap::AdvertisementCallbackParams_t *params)
{
    printf("SCANNING..%d",params->advertisingDataLen);
    int n1=params->advertisingDataLen;
    //printf("D1..%d",params->rssi);
    //printf("D2..%d:::",params->isScanResponse);
    for (int i=0; i<=n1; i++){
        printf("[%02x], ", params->advertisingData[i]);
    }
    if (params->advertisingData[15]==0x01)
    {
        AdvData[2]=0x01;
        AdvData[3]=0x01;
        AdvData[4]=0x01;
        printf("----on---------on----------on----");
    }
    printf("-------------------------on-------------------------");
}

/**
 * This function is called when the ble initialization process has failed
 */
void onBleInitError(BLE &ble, ble_error_t error)
{
    /* Initialization error handling should go here */
}

/**
 * Callback triggered when the ble initialization process has finished
 */
void bleInitComplete(BLE::InitializationCompleteCallbackContext *params)
{
    BLE&        ble   = params->ble;
    ble_error_t error = params->error;

    if (error != BLE_ERROR_NONE) {
        /* In case of error, forward the error handling to onBleInitError */
        onBleInitError(ble, error);
        return;
    }

    /* Ensure that it is the default instance of BLE */
    if(ble.getInstanceID() != BLE::DEFAULT_INSTANCE) {
        return;
    }

    /* Setup and start scanning */
    ble.gap().setScanParams(2000 /* scan interval */, 2000 /* scan window */);
}

// Restart Advertising PayLoad
void resetAdvertisingBLE()
{

    BLE::Instance(BLE::DEFAULT_INSTANCE).gap().stopAdvertising();
    BLE::Instance(BLE::DEFAULT_INSTANCE).gap().clearAdvertisingPayload();
    BLE::Instance(BLE::DEFAULT_INSTANCE).gap().accumulateAdvertisingPayload(GapAdvertisingData::COMPLETE_LOCAL_NAME, (uint8_t *)DEVICE_NAME, sizeof(DEVICE_NAME));
    BLE::Instance(BLE::DEFAULT_INSTANCE).gap().accumulateAdvertisingPayload(GapAdvertisingData::MANUFACTURER_SPECIFIC_DATA, (uint8_t *)&AdvData, sizeof(AdvData)); // Set data
    BLE::Instance(BLE::DEFAULT_INSTANCE).gap().accumulateAdvertisingPayload(GapAdvertisingData::COMPLETE_LIST_16BIT_SERVICE_IDS, (uint8_t *)uuid16_list, sizeof(uuid16_list)); // UUID's broadcast in advertising packet
    BLE::Instance(BLE::DEFAULT_INSTANCE).gap().setAdvertisingInterval(300);
    BLE::Instance(BLE::DEFAULT_INSTANCE).startAdvertising();
}

int main(void)
{
    printf("BLE Scanning Updated......\n");
    //ticker.attach(periodicCallback, 1);  /* trigger sensor polling every 2 seconds */
    BLE &ble = BLE::Instance();
    ble.init(bleInitComplete);
    while (true) {
        ble.waitForEvent();
        ble.gap().startScan(advertisementCallback);
        //wait(3);
        //ble.gap().stopScan();
        //wait(2);
        //resetAdvertisingBLE();
        //wait(2);

    }
}
