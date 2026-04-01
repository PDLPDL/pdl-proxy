package com.pdlpdl.pdlproxy.minecraft.gamestate.datagen;

import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMinecraftVersionAlignment {

    public static final String MINECRAFT_VERSION_PROPERTY_NAME = "minecraft.version";

    private static String buildMinecraftVersionProperty;

    @BeforeAll
    public static void beforeAll() {
        TestMinecraftVersionAlignment.buildMinecraftVersionProperty = System.getProperty(MINECRAFT_VERSION_PROPERTY_NAME);
    }

    @Test
    public void testMinecraftVersionAlignmentDatagenEmbeddedFiles() throws IOException {
        // First verify the library version matches the version specified for the build.
        String libraryMinecraftVersion = MinecraftCodec.CODEC.getMinecraftVersion();
        assertEquals(
            TestMinecraftVersionAlignment.buildMinecraftVersionProperty,
            libraryMinecraftVersion,
            "version of minecraft from the protocol lib is " +
                libraryMinecraftVersion +
                " MUST match our build's minecraft version, which is " +
                TestMinecraftVersionAlignment.buildMinecraftVersionProperty
        );


        // Next verify the version stored with datagen files matches the version specified for the build.  This acts
        //  as a catch to remind the developer that those files require update with every change of Minecraft version.
        try (InputStream dataGenInputStream = TestMinecraftVersionAlignment.class.getResourceAsStream("datagen-version.txt")) {
            byte[] data = new byte[4096];
            int len = dataGenInputStream.read(data);
            String contentAsString = new String(data, 0, len, StandardCharsets.UTF_8);

            assertEquals(
                TestMinecraftVersionAlignment.buildMinecraftVersionProperty,
                contentAsString,
                "version of minecraft from the build  MUST match the datagen-version.txt file in the build " +
                    " (make sure to update the built-in datagen files!)"
            );
        }
    }
}
