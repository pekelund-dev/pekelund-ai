package dev.pekelund.summoner.model;

/**
 * Request body sent to a familiar's POST /execute endpoint.
 */
public record ExecuteRequest(String target) {}
