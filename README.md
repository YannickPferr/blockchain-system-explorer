# Blockchain System Explorer
Currently supported Blockchains:
  - Ethereum

## How to use
1. Clone the repo
2. Execute the Jar File

Jar will automatically download and start all required dependencies. There are a few options regarding the start of the Java Application. You can either configure it via command line arguments or by passing it a config file. There are the following command line options:
```
-c,--config <file>                        Specify location of config file
-e,--ethNodes <ethereum node addresses>   Specify all ethereum nodes you
                                          want to connect to
-h,--help                                 Help flag to print usage
                                          message
-p,--props <property=value>               Specify the value of a property
```
  
You can configure a variety of settings:
```
ClientName = <Name of the Client: This specifies how the client will be identified in the DB entries> 
  Default: DefaultClient
InfluxURL = <Influx Address: This specifies the host where the InfluxDB is running> 
  Default: http://localhost:8086
EthNodes = <Ethereum Node Addresses: This specifies all ethererum nodes the client will connect to separated by commas. E.g.: node1,node2,node3> 
  Default: http://localhost:8545
DBName = <Name of the Database: This specifies how the DB will be named in InfluxDB> 
  Default: Blockchain
InfluxUser = <Influx User Name: This specifies the user used to authenticate to InfluxDB> 
  Default: root
InfluxPassword = <Influx Password: This specifies the password used to authenticate to InfluxDB> 
  Default: root
InfluxRetentionDuration = <Influx Retention Duration: This specifies the duration for which records are stored before they are deleted> 
  Default: INF (Unlimited Duration) 
InfluxRetentionReplication = <Influx Retention Duration: This specifies how many copies InfluxDB stores> 
  Default: 1
Log4jConfig = <Name of Log4j properties: This specifies the name and path of the Log4j properties file> 
  Default: log4j.properties
```
   
To change the default values, you can either use a config file or the --props command line option. You could for example start the application with --config cofig.properties and then have a config.properties file in which you set a different ClientName and a different Ethereum Node like this:
```
contents of config.properties
   
ClientName = TestClient
EthNodes = http://localhost:8545,http://localhost:8546
```
To do this via the command line, you start the application like this:
```
-pClientName=TestClient --ethNodes http://localhost:8545,http://localhost:8546
```

## Telegraf
If you want to also gather system data about the machine running the blockchain node, you need to install Telegraf on the remote and specify your Influx address in the Telegraf config file. You can download Telegraf [here](https://portal.influxdata.com/downloads/)
  
## Commands  
  ### Export data
  To export data into a csv file, you can use Influx Query Language in the Java client. E.g. type: 
   ```select * from Ethereum where BlockNumber = 1 ```
