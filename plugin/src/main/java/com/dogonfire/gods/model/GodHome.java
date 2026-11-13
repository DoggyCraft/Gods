package com.dogonfire.gods.model;

/**
 * Where a god lived before main altars: where its first altar was built, or set with /g sethome. In
 * gods.yml it is {@code Home.World}, {@code Home.X}, {@code Home.Y}, {@code Home.Z}. Kept as it is:
 * a god without a main altar keeps it as its home (see {@code HomeRules}); new gods get none, their
 * main altar is their home.
 */
public record GodHome(String world, double x, double y, double z) {}
