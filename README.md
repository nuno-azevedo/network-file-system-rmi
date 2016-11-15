# Network File-System

#### How to Run

###### 1° Java RMI
```bash
$ rmiregistry
```

###### 2° MetaDataServer
```bash
$ java MetaDataServer $METADATA_HOSTNAME
```

###### 3° StorageServer
```bash
$ java StorageServer $STORAGE_HOSTNAME $STORAGE_LOCAL_PATH $STORAGE_FILESYSTEM_PATH $METADATA_HOSTNAME
```

###### 4° Client
```bash
$ java Client $CONFIG_FILE $METADATA_HOSTNAME
```

#### Client Commands
- **pwd** : Shows the current directory
- **ls** *‘[path to file or directory]’* : Lists the contents of a directory in case of a directory, shows the file name in case of a file
- **cd** *‘[path to directory]’* : Changes the current directory
- **mkdir** *‘path to directory’* : Creates a new directory
- **touch** *‘path to file’* : Creates a new empty file
- **mv** *‘path to source file’* *‘path to target file’* : Moves the file located in ‘source’ to ‘target’ path
- **rm** *‘path to file or directory’* : Removes a directory and it's contents in case of a directory, removes a file in case of a file
- **open** *‘path to file’* : Opens a file with the application specified on configuration file apps.conf