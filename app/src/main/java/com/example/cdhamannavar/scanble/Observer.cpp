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
#include "toolchain.h"
#include "ble/BLE.h"
#include "TMP_nrf51/TMP_nrf51.h"

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
    struct AdvertisingData_t {
        uint8_t                        length; /* doesn't include itself */
        GapAdvertisingData::DataType_t dataType;
        uint8_t                        data[0];
    } PACKED;

    struct ApplicationData_t {
        uint16_t applicationSpecificId;             /* An ID used to identify temperature value
                                                       in the manufacture specific AD data field */
        TMP_nrf51::TempSensorValue_t tmpSensorValue; /* User defined application data */
    } PACKED;

    static const uint16_t APP_SPECIFIC_ID_TEST = 0xFEFE;

    /* Search for the manufacturer specific data with matching application-ID */
    AdvertisingData_t *pAdvData;
    size_t index = 0;
    while (index < params->advertisingDataLen) {
        pAdvData = (AdvertisingData_t *)&params->advertisingData[index];
        if (pAdvData->dataType == GapAdvertisingData::MANUFACTURER_SPECIFIC_DATA) {
            ApplicationData_t *pAppData = (ApplicationData_t *)pAdvData->data;
            if (pAppData->applicationSpecificId == APP_SPECIFIC_ID_TEST) {
                /* dump information on the console. */
                printf("From [%02x %02x %02x], ", params->peerAddr[2], params->peerAddr[1], params->peerAddr[0]);
                printf("Temp is %f\r\n", (TMP_nrf51::TempSensorValue_t)pAppData->tmpSensorValue);
                break;
            }
        }
        index += (pAdvData->length + 1);
    }
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
    ble.gap().setScanParams(1800 /* scan interval */, 1500 /* scan window */);
    ble.gap().startScan(advertisementCallback);
}

int main(void)
{
    ticker.attach(periodicCallback, 1);  /* trigger sensor polling every 2 seconds */

    BLE &ble = BLE::Instance();
    ble.init(bleInitComplete);

    while (true) {
        ble.waitForEvent();
    }
}
 
            