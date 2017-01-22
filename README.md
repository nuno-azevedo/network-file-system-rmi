# Network File-System

<br>
Implementation of a Distributed File-System that consists in three components:
* The meta-data server is responsible for storing the meta-data information relative to all storage servers. This unit only stores the real location of the files across the network file-system.
* The storage servers are responsible for storing the real data like files and directories, each one works like a single local file-systen. They inform the meta-data server of each action they do.
* The clients are an interface to any user control the file-system, they see the network file-system connected as if it was a unique file-system.


<br><br>
## Dependencies
- [JDK 8](https://docs.oracle.com/javase/8/docs/technotes/guides/install/linux_jdk.html)


<br><br>
## How to Build
```bash
$ make build
```


<br><br>
## How to Run

##### 1° Java RMI
```bash
$ rmiregistry
```

##### 2° Meta-Data Server
```bash
$ java MetaDataServer ${METADATA_HOSTNAME}
```

##### 3° Storage Server
```bash
$ java StorageServer ${METADATA_HOSTNAME} ${STORAGE_HOSTNAME} ${STORAGE_LOCAL_PATH} ${STORAGE_FILESYSTEM_PATH}
```

##### 4° Client
```bash
$ java Client ${METADATA_HOSTNAME} ${CONFIG_FILE}
```


<br><br>
## Clean Build Files
```bash
$ make clean
```


<br><br>
## Stop Running Executables
```bash
$ make stop
```


<br><br>
## Client Commands
- **pwd** : Shows the current directory
- **ls** *‘[path to file or directory]’* : Lists the contents of a directory in case of a directory, shows the file name in case of a file
- **cd** *‘[path to directory]’* : Changes the current directory
- **mkdir** *‘path to directory’* : Creates a new directory
- **touch** *‘path to file’* : Creates a new empty file
- **mv** *‘path to source file’* *‘path to target file’* : Moves the file located in ‘source’ to ‘target’ path
- **rm** *‘path to file or directory’* : Removes a directory and it's contents in case of a directory, removes a file in case of a file
- **open** *‘path to file’* : Opens a file with the application specified on the configuration file apps.conf
