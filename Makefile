JC = javac

SRC = src

METADATA = MetaDataServer
METADATA_HOST = machine0.example.com

STORAGE = StorageServer
STORAGE_HOST = machine1.example.com
STORAGE_REMOTE_PATH = /Storage

CLIENT = Client
CONFIG_FILE = conf/apps.conf


.SUFFIXES: .java .class


default: build

build: metadata storage client


metadata: MetaDataInterface.class MetaDataServer.class

MetaDataInterface.class: ${SRC}/MetaDataInterface.java
	${JC} -d . ${SRC}/MetaDataInterface.java ${SRC}/FSTree.java ${SRC}/Stat.java

MetaDataServer.class: ${SRC}/MetaDataServer.java
	${JC} -d . ${SRC}/MetaDataServer.java


storage: StorageInterface.class StorageServer.class

StorageInterface.class: ${SRC}/StorageInterface.java
	${JC} -d . ${SRC}/StorageInterface.java

StorageServer.class: MetaDataInterface.class ${SRC}/StorageServer.java
	${JC} -d . ${SRC}/StorageServer.java ${SRC}/FSTree.java


client: Client.class

Client.class: MetaDataInterface.class StorageInterface.class ${SRC}/Client.java
	${JC} -d . ${SRC}/Client.java


registry:
	rmiregistry


run-metadata: metadata
	java ${METADATA} ${METADATA_HOST}

run-storage: storage
	java ${STORAGE} ${METADATA_HOST} ${STORAGE_HOST} ~${STORAGE_REMOTE_PATH} ${STORAGE_REMOTE_PATH}

run-client: client
	java ${CLIENT} ${METADATA_HOST} ${CONFIG_FILE}


clean:
	rm -rf *.class


stop:
	- pkill rmiregistry
	$(shell jps | grep '${METADATA}\|${STORAGE}\|${CLIENT}' | awk '{ print $$1 }' | xargs kill -9)
