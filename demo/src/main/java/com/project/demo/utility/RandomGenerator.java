package com.project.demo.utility;

import java.security.SecureRandom;

public class RandomGenerator {

    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateUsername() {

        return "migrationtool_" + randomString(8);

    }

    public static String generatePassword() {

        return randomString(24);

    }

    public static String generateDatabase() {

        return "migrationdb_" + randomString(8);

    }

    private static String randomString(int length) {

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < length; i++) {

            builder.append(
                    CHARACTERS.charAt(
                            RANDOM.nextInt(CHARACTERS.length())
                    )
            );

        }

        return builder.toString();

    }

}