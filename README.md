# Scala Android Miner

A high performance and open source application for mining Scala on Android mobile devices.

![Screenshots](https://cdn.discordapp.com/attachments/663050624769982474/667120259387621396/sss.gif)

## Supported CPU instruction sets
- **armeabi-v7a**
- **arm64-v8a**

# Features	
We have implemented some major improvements and enhancements that differ from previous projects.	
- Temperature display	
- QR code feature to capture wallet addres	
- Completely redesigned UI	
- Refactored framework	
- Stats are available for pool using nodejs-pool code base and not limited to cryptonote-nodejs-pool code base pool only.

## Miner
This application is built upon the libraries of XLArig, the official Scala (XLA) CPU miner.
- Miner: [XLArig](https://github.com/scala-network/XLArig)
- Algorithm: [DefyX](https://medium.com/scala-network/scalas-v6-testnet-and-new-proof-of-work-information-3ba2a4eb0ad8)

## Usage
When opening the application, you first need to specify the pool that you want to use to mine XLA. Some predefined pools have already been configured to make it easier for you. You can still select 'custom' if you wish to use a pool that is not included in the predefined list. More information about the port number can be found on the pools' website, but if you are not sure you can use port '3333'.

Then, you have to specify the Scala wallet address that will be used with the mining pool. A validation has been added to this field to make sure you enter a valid Scala adress - if the adress is invalid, you won't be able to mine. You can also use the provided QR code functionnality to retrieve a valid Scala wallet address. Currently, only the [Official Pool](https://pool.scalaproject.io) has QR Code generator. You can also capture QR Code generated from our [Official GUI Wallet](https://github.com/scala-network/scala-electron-gui). Below demostrates how to get your address from the [Official Pool](https://pool.scalaproject.io) via QR Code.

## How to generate from pool.
![QR Code](https://cdn.discordapp.com/attachments/663050624769982474/667078766266155011/Peek_2020-01-16_02-52.gif)

### How to capture it via miner
![QR Code Capture](https://cdn.discordapp.com/attachments/663050624769982474/667118047693373450/ezgif-6-f742b856784b.gif)

The next field can be left blank if the pool doesn't use any options/password. When saving the settings, you will be redirected automatically to the Miner page. Just hit 'Start' to start mining.

The stats about the hashrate and the accepted shares are presented at the top of the screen and are retrieved from XLArig. The hahsrate information comes from the 60s hashrate data from the output log. The CPU and Battery temperatures are also displayed there and will be used in the future for the AMYAC integration. They still provide useful information to make sure your device is not overheating. The middle screen displays the output from XLArig as is.

You can use the buttons at the bottom of the application to display the hashrate in the log ('H'), to pause the miner ('P') and to resume the miner ('R').

The application will keep mining even when your phone/device is on standby mode. So keep in mind that even if your phone screen is off, the your device may still be running.


## Future development
- Add notifications when the miner is running in the background
- Integrate AMAYC protocol to prevent device from overheating
- Improve UI

## Donations
Donations setting has been set to 0% in the XLArig lib embedded within the application.
* XLA: `SEiTBcLGpfm3uj5b5RaZDGSUoAGnLCyG5aJjAwko67jqRwWEH26NFPd26EUpdL1zh4RTmTdRWLz8WCmk5F4umYaFByMtJT6RLjD6vzApQJWfi`
* XMR: `48edfHu7V9Z84YzzMa6fUueoELZ9ZRXq9VetWzYGzKt52XU5xvqgzYnDK9URnRoJMk1j8nLwEVsaSWJ4fhdUyZijBGUicoD`
* BTC: `1XTLY5LqdBXRW6hcHtnuMU7c68mAyW6qm`

## Credits
* Forked from [Mine2gether](https://github.com/Mine2Gether/m2g_android_miner)
* Original code from [MoneroMiner](https://github.com/upost/MoneroMiner)

## Contacts
* hello@scalaproject.io
* [Discord](https://discord.gg/djAFVvy)
* [Twitter](https://twitter.com/scalahq)
