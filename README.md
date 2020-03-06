Retriever
=========
This will load values from a service running at http://localhost:8080, and will store the values, compressed in the cache. Pre-fetched items will be supplied from the cache but, more data will be pulled from the source as required.

## Author
Mario Lyon (mario@digileo.com)

## Requirements
scala 2.12.10
sbt 

## Solution Overview
implemented with Akka-Http and Akka Typed Actors 

## Usage
In the root of the project execute
```bash
sbt run
```
This will launch a server that listens to requests on port 8081.

In another window, execute requests given the item number in the path:
```bash
curl localhost:8081/1000
```

Please note that the index starts at 0. 

## Executing Tests
execute the following from the root of the project:
```bash
sbt test
```
