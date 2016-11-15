METADATA = MetaDataServer
METADATA_HOST = machine0.example.com

STORAGE = StorageServer
STORAGE_HOST = machine1.example.com
STORAGE_REMOTE_PATH = /Storage

CLIENT = Client
CONFIG_FILE = conf/apps.conf

default: build

build:
	javac -d . src/*

clean:
	rm -rf *.class

registry:
	rmiregistry

metadata: build
	java ${METADATA} ${METADATA_HOST}

storage: build
	java ${STORAGE} ${METADATA_HOST} ${STORAGE_HOST} ${HOME} ${STORAGE_REMOTE_PATH}

client: build
	java ${CLIENT} ${METADATA_HOST} ${CONFIG_FILE}

stop:
	- pkill rmiregistry
	- pkill java
