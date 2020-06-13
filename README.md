# Kotlin Sqlite Bindings
This project provides kotlin multiplatform bindings for Sqlite3. (win, linux, mac, and Android)

By using it, you can write platform independent Sqlite code in your Kotlin common code.

**For now, it is more of a playground then a real project so do not use it on production**

## Disclaimer
This is **not** an official Google product.

## Project Structure
The project contains of two main modules.

### sqlitebindings
This module provides a very small shim over Sqlite APIs as a KMP project.

The only real implementation is on the native sourceSets (linux, windows, mac, android) while the JVM / Android ART
implementation simply delegates to native via JNI. The JNI is generated in the `jnigenerator` based on the `actual`
implementation for `SqliteApi` in the `jvm/art` source.

It also bundles Sqlite as a static lib (for native) and dynamic lib (for JVM).

### ksqlite3
This module aims to provide higher level APIs for SQLite and internally depends on `sqlitebindings` module.

## Artifacts
There are no release artifacts right now but the CI builds a `repo` folder that can serve as a
maven repository. If you want to try, you can download the `combined-repo` artifact from the
latest Github Action and add it as a maven repo to your project.
## Disclaimer
In case you've not read it above, this project is really a playground at this point. I'm trying to learn KMP so there is
probably lots of memory handling etc mistakes. That being said, I hope to grow this into a proper project eventually.

Copyright:

    Copyright 2020 Google LLC

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.