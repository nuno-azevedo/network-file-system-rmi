# Network File-System

### Como executar

##### 1 - Java RMI
```bash
$ rmiregistry
```

##### 2 - MetaDataServer
```bash
$ java MetaDataServer $METADATA_HOSTNAME
```

##### 3 - StorageServer
```bash
$ java StorageServer $STORAGE_HOSTNAME $STORAGE_LOCAL_PATH $STORAGE_FILESYSTEM_PATH $METADATA_HOSTNAME
```

##### 4 - Client
```bash
$ java Client $CONFIG_FILE $METADATA_HOSTNAME
```
