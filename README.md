# Copyright

Copyright (c) 2021 Playful Digital Learning LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

# License

Please see the LICENSE file.

# Intended Usage

While Playful Digital Learning provides this software as opensource for your enjoyment, it is intended for use in
building advanced features for Minecraft, such as programmable BOTs, for user's enjoyment.

Please refrain from attempting to using this software to implement cheats.  Also, please obtain permission from
server owners before connecting this proxy to their servers.


# PDL-PROXY

Playful Digital Learning brings this Minecraft Proxy Server that provides functionality on top of the normal Minecraft
Server without modifying the Server.

# Features

* [x] Logging of packets with timestamp to file
* [x] Compression of log files
* [x] API for embedding the proxy either for modding, or for packing with other software
* [x] Pluggable interceptors which can do all of the following with the packet flow:
  * [x] View content of packets
  * [x] Filter out packets
  * [x] Add new packets
  
# Limitations

* Downstream server Login is not supported (only offline servers work as-is)
  * Note this can be changed in-code, but the Proxy server currently, as-written, does not support it
  * The MCProtocolLib, that is used to connect to the downstream server, does support authentication
* Only a single downstream server can be used with one instance of the Proxy server (as written)

# Command-Line Usage

    # Listen on 0.0.0.0:7777 and forward connections to server at localhost:25565
    $ PROJECT_VERSION="$(mvn -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive -q org.codehaus.mojo:exec-maven-plugin:1.6.0:exec)"
    $ java -jar pdl-proxy-main/target/pdl-proxy-main-"${PROJECT_VERSION}".jar  -L 0.0.0.0:7777 -F localhost:25565

    # Add compressed trace log files 
    $ java -jar pdl-proxy-main/target/pdl-proxy-main-"${PROJECT_VERSION}".jar  -L 0.0.0.0:7777 -F localhost:25565 -t -d trace.d -c

## Command-Line Arguments

    usage: Main [options]
        -c,--compress                  Compress the tracing files (see --trace)
        -d,--trace-output-dir <arg>    Output directory into which tracing files will be written (see --trace)
        -F,--forward-address <arg>     Address of server to which client connections are forwarded, in [<host>][:<port>]
                                       format
        -h,--help                      Display command-line help
        -L,--listen-address <arg>      Address on which to listen for connections in [<host>][:<port>] format
        -P,--POC                       Enable Proof-Of-Concept Code
        -t,--trace                     Enable tracing of packets to file

