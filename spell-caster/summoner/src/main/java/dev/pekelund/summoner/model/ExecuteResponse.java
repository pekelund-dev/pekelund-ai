package dev.pekelund.summoner.model;

/**
 * Response from a familiar's POST /execute endpoint.
 */
public record ExecuteResponse(String familiar, String pattern, String result) {}
