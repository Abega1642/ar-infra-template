package com.example.arinfra.file;

import com.example.arinfra.InfraGenerated;

/**
 * Immutable data carrier representing a file's cryptographic hash.
 *
 * <p>This record encapsulates both the hash algorithm used (e.g., MD5, SHA-256) and the resulting
 * hash value. Used for file integrity verification, deduplication, and content validation in cloud
 * storage operations.
 *
 * @param algorithm the hashing algorithm used (e.g., "MD5", "SHA-256", "SHA-512")
 * @param value the hexadecimal string representation of the computed hash
 */
@InfraGenerated
public record FileHash(String algorithm, String value) {}
