# Scala Android Miner

A high performance and open-source application for mining Scala on Android mobile devices.

<p align="left">
  <img src="https://scalaproject.io/assets/img/mobile-minerv2.png" width="350" title="hover text">
</p>

## Supported CPU instruction sets
- **armeabi-v7a**
- **arm64-v8a**

## Features		
- AMAYC machine-learning algorithm to protect the device
- Temperature display
- QR code feature to capture wallet address
- Completely redesigned UI
- Refactored framework
- Payout widget
- Live Pool statistics for pools based on nodejs-pool cryptonote-nodejs-pool frameworks

## Miner
This application is built upon the libraries of XLArig, the official Scala (XLA) CPU miner.
- Miner: [XLArig](https://github.com/scala-network/XLArig)
- Algorithm: [DefyX](https://medium.com/scala-network/scalas-v6-testnet-and-new-proof-of-work-information-3ba2a4eb0ad8)

## Usage
### Settings
#### Pool
When opening the application for the first time, you have to specify the pool that you want to use to mine XLA. Some predefined pools have been configured to make it easier for you. You can still select 'custom' if you wish to use a pool that is not included in the predefined list. More information about the port number can be found on the pools' website, but if you are not sure you can use port '3333'.

The 'Worker name or pool options' field can be left blank if the pool doesn't use any options/password. In this case, the worker name will be set to your device type (make and model) by default so you can easily identify it on the pool.

#### Wallet Address
Specifies the Scala wallet address that will be used with the mining pool. A validation has been added to this field to make sure you enter a valid address. You can also use the provided QR code functionnality to retrieve the address. Currently, only the [Official Pool](https://pool.scalaproject.io) and the [Official GUI Wallet](https://github.com/scala-network/scala-electron-gui) support QR code functionnality.

#### Hardware Settings
##### CPU Cores
The application automatically detects your device's number of CPU cores. Use less cores if you want to prevent your device from overheating or to keep using other applications while mining.

##### CPU Temperature
Defines the maximum temperature that you want your device's CPU to reach. If your device has no CPU temperature sensor, this setting will be ignored.

##### Battery Temperature
Defines the maximum temperature that you want your device's battery to reach. If your device has no Battery temperature sensor, this setting will be ignored.

##### Cooldown Threshold
Defines the temperature at which the application will resume the mining process. Both the CPU and Battery temperatures must have reached the safe level for the miner to resume. For example, if the maximum CPU and battery temperatures are respectively 65 °C and 40 °C and the Cooldown Threshold is set to -10%, then the CPU and battery temperatures must reach (0.9 * 65) °C and (0.9 * 40) °C for the miner to resume.

##### Disable Temperature Control
This application implements two layers of protection for your device. The first one is the As-Much-As-You-Can (AMAYC) machine learning algorithm that can predict when your device will overheat and pause the miner until the device's temperature has normalized. The second one is a static temperature monitoring that will automatically pause the miner if your device reaches one of the maximum temperatures defined in the settings. **Turning this feature off might cause damage to your device. Do it at your own risk!**

#### Options
##### Mining Goal
The mining goal should reflect the pool payout setting, but you can also specify a custom value for your device. The default value is the minimum payout value of the specified mining pool.

##### Pause mining on battery power
Enable this feature to pause mining when your device is not charging.

When saving the settings, you will be redirected automatically to the Miner page. Just hit 'Start' to start mining.

### Miner

The Payout Widget which is at the top of the screen displays the current balance for the selected pool and the progression toward the defined mining goal. If the specified pool does not provide an API (in the case of custom pools for example), this widget will be hidden.

The stats about the hashrate and the accepted shares are then presented in the Device Widget. All the information is extracted from XLArig. The hahsrate information comes from the 15s/30s/60s hashrate data from the output log, in this order of priority. The CPU and Battery temperatures are used for the AMYAC integration and static temperature protection. The middle screen displays the output log from XLArig as is.

You can use the buttons at the bottom of the application to display the hashrate in the log ('H'), to pause the miner ('P') and to resume the miner ('R'). You cannot resume the miner when the device is cooling.

The application will keep mining even when your device is on standby mode. So keep in mind that even if your phone screen is off, your device may still be running.

## Future development
- Push notifications
- Wallet integration
- Unified design for the all-in-one application

## Donations
Donations setting has been set to 0% in the XLArig lib embedded within the application.
* XLA: `SEiTBcLGpfm3uj5b5RaZDGSUoAGnLCyG5aJjAwko67jqRwWEH26NFPd26EUpdL1zh4RTmTdRWLz8WCmk5F4umYaFByMtJT6RLjD6vzApQJWfi`
* BTC: `1XTLY5LqdBXRW6hcHtnuMU7c68mAyW6qm`
* ETH: `0x133a15dF7177823Dd407ca87A190bbE4585a379e`

## Credits
* Forked from [Mine2gether](https://github.com/Mine2Gether/m2g_android_miner)
* Original code from [MoneroMiner](https://github.com/upost/MoneroMiner)

## Contacts
* [Website](https://scalaproject.io/)
* hello@scalaproject.io
* [Discord](https://discord.gg/djAFVvy)
* [Twitter](https://twitter.com/scalahq)


# License

XLARig and Mobile Miner is licensed as GPLv3, thus this derivative work also is. You need to consider this if you plan to publish an Android application. You'd propably need to make it GPLv3 also, unless you can somehow make use of the GPL clause which allows to bundle a GPLv3 binary with another proprietary licensed binary.
