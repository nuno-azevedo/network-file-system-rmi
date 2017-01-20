# Network File-System

## How to Build
```bash
$ javac -d . src/*
```

<br><br/>
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

<br><br/>
## Client Commands
- **pwd** : Shows the current directory
- **ls** *‘[path to file or directory]’* : Lists the contents of a directory in case of a directory, shows the file name in case of a file
- **cd** *‘[path to directory]’* : Changes the current directory
- **mkdir** *‘path to directory’* : Creates a new directory
- **touch** *‘path to file’* : Creates a new empty file
- **mv** *‘path to source file’* *‘path to target file’* : Moves the file located in ‘source’ to ‘target’ path
- **rm** *‘path to file or directory’* : Removes a directory and it's contents in case of a directory, removes a file in case of a file
- **open** *‘path to file’* : Opens a file with the application specified on configuration file apps.conf
