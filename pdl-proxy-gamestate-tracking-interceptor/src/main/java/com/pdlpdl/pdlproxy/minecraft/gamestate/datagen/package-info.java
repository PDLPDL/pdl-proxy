/**
 * Minecraft server and client have built-in data shared by version, which acts as defaults that the client and server
 *  then agree to use unless explicitly overridden.  This package contains the logic to read and make those defaults
 *  available where needed.
 *
 *  For example, the world height and min_y values for each dimension_type.
 *
 *  Note that defaults of these files are generated from the server jar file during the build and stored in the JAR file
 *   as a fallback.  However, it is recommended for applications to instead dynamically generate the needed reports
 *   and load those files at startup time for the greatest flexibility and reduced risk of needing to rebuild the
 *   sources due to change.
 */
package com.pdlpdl.pdlproxy.minecraft.gamestate.datagen;